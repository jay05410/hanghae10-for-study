package io.hhplus.ecommerce.product.infra.statistics

import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.product.application.port.out.ProductStatisticsPort
import io.hhplus.ecommerce.product.domain.calculator.PopularityCalculator
import io.hhplus.ecommerce.product.domain.event.ProductStatisticsEvent
import io.hhplus.ecommerce.product.domain.vo.ProductStatsVO
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Redis 기반 상품 통계 어댑터
 *
 * ProductStatisticsPort의 Redis 구현체.
 * 실시간 통계 데이터를 Redis에 저장하고 조회함.
 *
 * 아키텍처:
 * 1. 조회/판매/찜 -> 이벤트 발생 -> Redis 로그 저장 (즉시)
 * 2. 별도 스케줄러가 로그를 청크 단위로 읽어서 -> DB 벌크 업데이트
 * 3. 실시간 집계는 Redis 로그에서 직접 계산
 *
 * 데이터 동기화 전략:
 * - Redis: 실시간 조회용 캐시 (최대 15분 TTL)
 * - DB: 영구 저장 및 히스토리 분석용 (30분마다 배치 업데이트)
 * - 데이터 불일치: Redis가 최신, DB는 지연 반영 (허용 범위: 최대 30분)
 * - 장애 복구: Redis 손실 시 DB에서 복구 가능 (일부 실시간 데이터 손실 허용)
 *
 * Redis 키:
 * - 모든 키는 RedisKeyNames.Stats에서 중앙 관리
 */
@Component
class RedisProductStatisticsAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : ProductStatisticsPort {

    override fun recordViewEvent(productId: Long, userId: Long?): Long {
        val event = ProductStatisticsEvent.ProductViewed(
            pId = productId,
            userId = userId
        )

        // 1. 이벤트 로그 저장 (영구 보관용)
        storeEventLog(event)

        // 2. 실시간 집계용 타임스탬프 기록
        val minute = getCurrentMinute()
        val realtimeKey = RedisKeyNames.Stats.viewKey(productId, minute)
        redisTemplate.opsForSet().add(realtimeKey, "${event.timestamp}:${userId ?: "anonymous"}")
        redisTemplate.expire(realtimeKey, java.time.Duration.ofMinutes(15))

        // 3. 인기 상품 Sorted Set 업데이트 (O(log N) 연산)
        updatePopularProductsScore(productId)

        // 4. 최근 10분 조회수 반환
        return getLast10MinuteViews(productId)
    }

    /**
     * 인기 상품 점수 업데이트
     * 10분 단위 윈도우로 관리하여 실시간성 유지
     */
    private fun updatePopularProductsScore(productId: Long) {
        val windowId = System.currentTimeMillis() / (10 * 60 * 1000)
        val windowKey = RedisKeyNames.Stats.popularWindowKey(windowId)

        // 현재 윈도우의 Sorted Set에 점수 증가
        redisTemplate.opsForZSet().incrementScore(windowKey, productId.toString(), 1.0)
        redisTemplate.expire(windowKey, java.time.Duration.ofMinutes(15))
    }

    override fun recordSalesEvent(productId: Long, quantity: Int, orderId: Long): ProductStatsVO {
        val event = ProductStatisticsEvent.ProductSold(
            pId = productId,
            quantity = quantity,
            orderId = orderId
        )

        // 1. 이벤트 로그 저장
        storeEventLog(event)

        // 2. 실시간 판매량 누적 (영구 저장)
        val salesKey = RedisKeyNames.Stats.salesKey(productId)
        redisTemplate.opsForValue().increment(salesKey, quantity.toLong())

        // 3. 실시간 통계 조회 및 VO 반환
        val (viewCount, salesCount, wishCount) = getRealTimeStats(productId)

        return ProductStatsVO.create(
            productId = productId,
            viewCount = viewCount,
            salesCount = salesCount,
            hotScore = PopularityCalculator.calculateScore(salesCount, viewCount, wishCount)
        )
    }

    override fun recordWishEvent(productId: Long, userId: Long): Long {
        val event = ProductStatisticsEvent.ProductWished(
            pId = productId,
            userId = userId
        )

        // 1. 이벤트 로그 저장
        storeEventLog(event)

        // 2. 실시간 찜 개수 관리 (Set으로 중복 방지)
        val wishKey = RedisKeyNames.Stats.wishKey(productId)
        redisTemplate.opsForSet().add(wishKey, userId.toString())

        return redisTemplate.opsForSet().size(wishKey) ?: 0L
    }

    override fun recordUnwishEvent(productId: Long, userId: Long): Long {
        val event = ProductStatisticsEvent.ProductUnwished(
            pId = productId,
            userId = userId
        )

        // 1. 이벤트 로그 저장
        storeEventLog(event)

        // 2. 실시간 찜 개수에서 제거
        val wishKey = RedisKeyNames.Stats.wishKey(productId)
        redisTemplate.opsForSet().remove(wishKey, userId.toString())

        return redisTemplate.opsForSet().size(wishKey) ?: 0L
    }

    override fun getLast10MinuteViews(productId: Long): Long {
        val currentMinute = getCurrentMinute()
        var totalViews = 0L

        // 최근 10분간의 조회수 합산
        for (i in 0..9) {
            val minute = currentMinute - i
            val realtimeKey = RedisKeyNames.Stats.viewKey(productId, minute)
            val viewCount = redisTemplate.opsForSet().size(realtimeKey) ?: 0L
            totalViews += viewCount
        }

        return totalViews
    }

    override fun getRealTimeStats(productId: Long): Triple<Long, Long, Long> {
        val viewCount = getLast10MinuteViews(productId)

        val salesKey = RedisKeyNames.Stats.salesKey(productId)
        val salesCount = redisTemplate.opsForValue().get(salesKey)?.toString()?.toLong() ?: 0L

        val wishKey = RedisKeyNames.Stats.wishKey(productId)
        val wishCount = redisTemplate.opsForSet().size(wishKey) ?: 0L

        return Triple(viewCount, salesCount, wishCount)
    }

    override fun getRealTimePopularProducts(limit: Int): List<Pair<Long, Long>> {
        val currentWindowId = System.currentTimeMillis() / (10 * 60 * 1000)

        // 현재 윈도우와 이전 윈도우 키 (최근 ~20분 데이터)
        val currentWindowKey = RedisKeyNames.Stats.popularWindowKey(currentWindowId)
        val previousWindowKey = RedisKeyNames.Stats.popularWindowKey(currentWindowId - 1)

        // 두 윈도우를 합산한 결과 조회 (ZUNIONSTORE 대신 두 Set 병합)
        val result = mutableMapOf<Long, Double>()

        // 현재 윈도우에서 상위 상품 조회
        redisTemplate.opsForZSet()
            .reverseRangeWithScores(currentWindowKey, 0, (limit * 2).toLong() - 1)
            ?.forEach { tuple ->
                val productId = tuple.value?.toString()?.toLongOrNull() ?: return@forEach
                val score = tuple.score ?: 0.0
                result[productId] = result.getOrDefault(productId, 0.0) + score
            }

        // 이전 윈도우에서 상위 상품 조회 (합산)
        redisTemplate.opsForZSet()
            .reverseRangeWithScores(previousWindowKey, 0, (limit * 2).toLong() - 1)
            ?.forEach { tuple ->
                val productId = tuple.value?.toString()?.toLongOrNull() ?: return@forEach
                val score = tuple.score ?: 0.0
                result[productId] = result.getOrDefault(productId, 0.0) + score
            }

        // 합산 점수로 정렬 후 상위 limit개 반환
        return result.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value.toLong() }
    }

    /**
     * 이벤트 로그를 Redis에 저장 (청크 처리용)
     */
    private fun storeEventLog(event: ProductStatisticsEvent) {
        val eventJson = objectMapper.writeValueAsString(event)
        val logKey = RedisKeyNames.Stats.eventLogKey(getCurrentHour())

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
