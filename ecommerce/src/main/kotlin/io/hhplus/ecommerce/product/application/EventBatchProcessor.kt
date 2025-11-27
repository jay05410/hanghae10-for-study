package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.event.ProductStatisticsEvent
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 이벤트 배치 처리기 - 청크 단위 벌크 업데이트
 *
 * 동작 방식:
 * 1. Redis에서 이벤트 로그를 시간 단위로 읽어옴
 * 2. 상품별로 그룹핑하여 집계 계산
 * 3. 청크 단위로 DB 벌크 업데이트
 * 4. 처리 완료된 로그는 삭제
 *
 * 장점:
 * - Write-back 없이 단방향 처리
 * - 청크 단위로 락 경합 최소화
 * - 이벤트 손실 방지
 */
@Component
class EventBatchProcessor(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val productStatisticsRepository: ProductStatisticsRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val EVENT_LOG_PREFIX = "stats:events"
        private const val CHUNK_SIZE = 100
    }

    /**
     * 30분마다 이벤트 배치 처리
     *
     * 주기를 길게 설정하여 충분한 이벤트를 모아서 효율적 처리
     */
    @Scheduled(fixedDelay = 1_800_000L) // 30분
    fun processEventBatch() {
        logger.info("[EventBatch] 이벤트 배치 처리 시작")

        val startTime = System.currentTimeMillis()
        var totalProcessed = 0

        try {
            // 1. 처리할 시간 범위 결정 (현재 시간 - 1시간까지)
            val currentHour = getCurrentHour()
            val targetHours = (1..2).map { currentHour - it } // 1~2시간 전 데이터 처리

            targetHours.forEach { hour ->
                val processed = processEventsForHour(hour)
                totalProcessed += processed
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info("[EventBatch] 배치 처리 완료: ${totalProcessed}건 처리, ${duration}ms 소요")

        } catch (e: Exception) {
            logger.error("[EventBatch] 배치 처리 실패: ${e.message}", e)
        }
    }

    /**
     * 특정 시간의 이벤트들을 처리
     *
     * @param hour 처리할 시간 (UNIX timestamp / 3600)
     * @return 처리된 이벤트 수
     */
    private fun processEventsForHour(hour: Long): Int {
        val logKey = "$EVENT_LOG_PREFIX:$hour"

        // Redis에서 해당 시간의 모든 이벤트 읽어오기
        val eventLogs = redisTemplate.opsForList().range(logKey, 0, -1) ?: return 0

        if (eventLogs.isEmpty()) {
            logger.debug("[EventBatch] 시간 $hour: 처리할 이벤트 없음")
            return 0
        }

        logger.info("[EventBatch] 시간 $hour: ${eventLogs.size}개 이벤트 처리 시작")

        // 이벤트 파싱 및 집계
        val aggregatedStats = aggregateEvents(eventLogs)

        // 청크 단위로 벌크 업데이트
        val chunks = aggregatedStats.entries.chunked(CHUNK_SIZE)
        chunks.forEach { chunk ->
            bulkUpdateStatistics(chunk)
        }

        // 처리 완료된 로그 삭제
        redisTemplate.delete(logKey)

        logger.info("[EventBatch] 시간 $hour: ${eventLogs.size}개 이벤트 처리 완료")
        return eventLogs.size
    }

    /**
     * 이벤트 로그들을 상품별로 집계
     *
     * @param eventLogs Redis에서 읽어온 이벤트 로그들
     * @return 상품별 집계 데이터 맵
     */
    private fun aggregateEvents(eventLogs: List<Any>): Map<Long, AggregatedStats> {
        val statsMap = mutableMapOf<Long, AggregatedStats>()

        eventLogs.forEach { eventLog ->
            try {
                val eventJson = eventLog.toString()
                val eventMap = objectMapper.readValue(eventJson, Map::class.java)

                // 이벤트 타입과 상품 ID 추출
                val productId = (eventMap["pId"] as Number).toLong()
                val currentStats = statsMap.getOrPut(productId) { AggregatedStats() }

                when {
                    eventJson.contains("ProductViewed") -> {
                        currentStats.viewCount += 1
                    }
                    eventJson.contains("ProductSold") -> {
                        val quantity = (eventMap["quantity"] as Number).toInt()
                        currentStats.salesCount += quantity
                    }
                    eventJson.contains("ProductWished") -> {
                        currentStats.wishCount += 1
                    }
                    eventJson.contains("ProductUnwished") -> {
                        currentStats.wishCount -= 1 // 찜 해제
                    }
                }

            } catch (e: Exception) {
                logger.warn("[EventBatch] 이벤트 파싱 실패: ${e.message}")
            }
        }

        return statsMap
    }

    /**
     * 청크 단위 벌크 업데이트
     *
     * @param chunk 처리할 청크 데이터
     */
    @Transactional
    private fun bulkUpdateStatistics(chunk: List<Map.Entry<Long, AggregatedStats>>) {
        try {
            chunk.forEach { (productId, aggregatedStats) ->
                var statistics = productStatisticsRepository.findByProductId(productId)

                if (statistics == null) {
                    statistics = ProductStatistics.create(productId)
                }

                // 집계된 값 반영
                if (aggregatedStats.viewCount > 0) {
                    statistics.addViewCount(aggregatedStats.viewCount)
                }
                if (aggregatedStats.salesCount > 0) {
                    statistics.addSalesCount(aggregatedStats.salesCount.toLong())
                }
                if (aggregatedStats.wishCount != 0L) { // 양수/음수 모두 처리
                    statistics.addWishCount(aggregatedStats.wishCount)
                }

                productStatisticsRepository.save(statistics)
            }

            logger.debug("[EventBatch] 청크 업데이트 완료: ${chunk.size}건")

        } catch (e: Exception) {
            logger.error("[EventBatch] 청크 업데이트 실패: ${e.message}", e)
            throw e
        }
    }

    /**
     * 현재 시간(hour) 반환
     */
    private fun getCurrentHour(): Long {
        return System.currentTimeMillis() / (60 * 60 * 1000)
    }

    /**
     * 집계된 통계 데이터 클래스
     */
    private data class AggregatedStats(
        var viewCount: Long = 0,
        var salesCount: Long = 0,
        var wishCount: Long = 0
    )
}