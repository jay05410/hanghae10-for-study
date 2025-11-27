package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.event.ProductStatisticsEvent
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 이벤트 기반 통계 서비스 - 단방향 흐름
 *
 * 아키텍처:
 * 1. 조회/판매/찜 → 이벤트 발생 → Redis 로그 저장 (즉시)
 * 2. 별도 스케줄러가 로그를 청크 단위로 읽어서 → DB 벌크 업데이트
 * 3. 실시간 집계는 Redis 로그에서 직접 계산
 *
 * 장점:
 * - Write-back 제거로 로직 단방향화
 * - 이벤트 손실 없이 안정적 저장
 * - 실시간성과 성능 양립
 */
@Service
class EventBasedStatisticsService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val EVENT_LOG_PREFIX = "stats:events"
        private const val REALTIME_VIEW_PREFIX = "stats:realtime:view"
        private const val REALTIME_SALES_PREFIX = "stats:realtime:sales"
        private const val REALTIME_WISH_PREFIX = "stats:realtime:wish"
    }

    /**
     * 조회 이벤트 기록
     *
     * @param productId 상품 ID
     * @param userId 사용자 ID (선택적)
     * @return 실시간 조회수 (최근 10분)
     */
    fun recordViewEvent(productId: Long, userId: Long? = null): Long {
        val event = ProductStatisticsEvent.ProductViewed(
            pId = productId,
            userId = userId
        )

        // 1. 이벤트 로그 저장 (영구 보관용)
        storeEventLog(event)

        // 2. 실시간 집계용 타임스탬프 기록
        val minute = getCurrentMinute()
        val realtimeKey = "$REALTIME_VIEW_PREFIX:$productId:$minute"
        redisTemplate.opsForSet().add(realtimeKey, "${event.timestamp}:${userId ?: "anonymous"}")
        redisTemplate.expire(realtimeKey, java.time.Duration.ofMinutes(15))

        // 3. 최근 10분 조회수 반환
        return getLast10MinuteViews(productId)
    }

    /**
     * 판매 이벤트 기록
     *
     * @param productId 상품 ID
     * @param quantity 판매 수량
     * @param orderId 주문 ID
     * @return 실시간 총 판매량
     */
    fun recordSalesEvent(productId: Long, quantity: Int, orderId: Long): Long {
        val event = ProductStatisticsEvent.ProductSold(
            pId = productId,
            quantity = quantity,
            orderId = orderId
        )

        // 1. 이벤트 로그 저장
        storeEventLog(event)

        // 2. 실시간 판매량 누적 (영구 저장)
        val salesKey = "$REALTIME_SALES_PREFIX:$productId"
        return redisTemplate.opsForValue().increment(salesKey, quantity.toLong()) ?: quantity.toLong()
    }

    /**
     * 찜 이벤트 기록
     *
     * @param productId 상품 ID
     * @param userId 사용자 ID
     * @return 현재 찜 개수
     */
    fun recordWishEvent(productId: Long, userId: Long): Long {
        val event = ProductStatisticsEvent.ProductWished(
            pId = productId,
            userId = userId
        )

        // 1. 이벤트 로그 저장
        storeEventLog(event)

        // 2. 실시간 찜 개수 관리 (Set으로 중복 방지)
        val wishKey = "$REALTIME_WISH_PREFIX:$productId"
        redisTemplate.opsForSet().add(wishKey, userId.toString())

        return redisTemplate.opsForSet().size(wishKey) ?: 0L
    }

    /**
     * 찜 해제 이벤트 기록
     */
    fun recordUnwishEvent(productId: Long, userId: Long): Long {
        val event = ProductStatisticsEvent.ProductUnwished(
            pId = productId,
            userId = userId
        )

        // 1. 이벤트 로그 저장
        storeEventLog(event)

        // 2. 실시간 찜 개수에서 제거
        val wishKey = "$REALTIME_WISH_PREFIX:$productId"
        redisTemplate.opsForSet().remove(wishKey, userId.toString())

        return redisTemplate.opsForSet().size(wishKey) ?: 0L
    }

    /**
     * 최근 10분 실시간 조회수 계산
     */
    fun getLast10MinuteViews(productId: Long): Long {
        val currentMinute = getCurrentMinute()
        var totalViews = 0L

        // 최근 10분간의 조회수 합산
        for (i in 0..9) {
            val minute = currentMinute - i
            val realtimeKey = "$REALTIME_VIEW_PREFIX:$productId:$minute"
            val viewCount = redisTemplate.opsForSet().size(realtimeKey) ?: 0L
            totalViews += viewCount
        }

        return totalViews
    }

    /**
     * 실시간 통계 조회
     *
     * @param productId 상품 ID
     * @return Triple(최근 10분 조회수, 총 판매량, 총 찜 개수)
     */
    fun getRealTimeStats(productId: Long): Triple<Long, Long, Long> {
        val viewCount = getLast10MinuteViews(productId)

        val salesKey = "$REALTIME_SALES_PREFIX:$productId"
        val salesCount = redisTemplate.opsForValue().get(salesKey)?.toString()?.toLong() ?: 0L

        val wishKey = "$REALTIME_WISH_PREFIX:$productId"
        val wishCount = redisTemplate.opsForSet().size(wishKey) ?: 0L

        return Triple(viewCount, salesCount, wishCount)
    }

    /**
     * 실시간 인기 상품 조회 (최근 10분 기준)
     *
     * @param limit 조회할 상품 수
     * @return 인기순 상품 ID와 조회수 목록
     */
    fun getRealTimePopularProducts(limit: Int): List<Pair<Long, Long>> {
        // 활성 상품 목록에서 실시간 조회수 계산
        val productStats = mutableMapOf<Long, Long>()

        // Redis SCAN을 통해 활성 상품 찾기 (예시로 1-1000 범위)
        for (productId in 1L..1000L) {
            val viewCount = getLast10MinuteViews(productId)
            if (viewCount > 0) {
                productStats[productId] = viewCount
            }
        }

        return productStats.toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * 이벤트 로그를 Redis에 저장 (청크 처리용)
     */
    private fun storeEventLog(event: ProductStatisticsEvent) {
        val eventJson = objectMapper.writeValueAsString(event)
        val logKey = "$EVENT_LOG_PREFIX:${getCurrentHour()}"

        // 시간별로 로그 분리하여 저장 (처리 효율성을 위해)
        redisTemplate.opsForList().rightPush(logKey, eventJson)
        redisTemplate.expire(logKey, java.time.Duration.ofDays(7)) // 7일 보관
    }

    /**
     * 현재 분(minute) 반환
     */
    private fun getCurrentMinute(): Long {
        return System.currentTimeMillis() / (60 * 1000)
    }

    /**
     * 현재 시간(hour) 반환
     */
    private fun getCurrentHour(): Long {
        return System.currentTimeMillis() / (60 * 60 * 1000)
    }
}