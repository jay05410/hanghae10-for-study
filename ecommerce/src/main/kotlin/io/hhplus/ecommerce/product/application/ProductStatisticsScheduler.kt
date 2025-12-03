package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.product.domain.entity.ProductPermanentStatistics
import io.hhplus.ecommerce.product.domain.event.ProductStatisticsEvent
import io.hhplus.ecommerce.product.domain.repository.ProductPermanentStatisticsRepository
import io.hhplus.ecommerce.product.usecase.GetProductQueryUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 통합 상품 통계 스케줄러
 *
 * 기존 EventBatchProcessor와 PopularProductsCacheWarmer를 하나로 통합하여:
 * - 리소스 경합 방지 (단일 스케줄러)
 * - 순차 실행으로 안정성 보장
 * - 가독성 향상 및 유지보수성 개선
 *
 * 실행 순서:
 * 1. 이벤트 배치 처리 (Redis → DB)
 * 2. 인기 상품 캐시 갱신 (최신 데이터 반영)
 */
@Component
class ProductStatisticsScheduler(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val productPermanentStatisticsRepository: ProductPermanentStatisticsRepository,
    private val objectMapper: ObjectMapper,
    private val getProductQueryUseCase: GetProductQueryUseCase,
    private val cacheManager: CacheManager
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val CHUNK_SIZE = 100
        private val WARM_UP_LIMITS = listOf(5, 10, 20)
    }

    /**
     * 30분마다 통합 스케줄링 실행
     *
     * 실행 순서:
     * 1. 이벤트 배치 처리 → DB 업데이트
     * 2. 캐시 워밍 → 최신 데이터로 캐시 갱신
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000L) // 30분
    fun processStatisticsAndWarmCache() {
        logger.info("[통합스케줄러] 상품 통계 처리 시작")

        val startTime = System.currentTimeMillis()

        try {
            // 1. 이벤트 배치 처리
            val processedEvents = processEventBatch()

            // 2. 인기 상품 캐시 워밍 (최신 데이터 반영)
            warmPopularProductsCache()

            val duration = System.currentTimeMillis() - startTime
            logger.info("[통합스케줄러] 처리 완료: ${processedEvents}건 이벤트 처리, ${duration}ms 소요")

        } catch (e: Exception) {
            logger.error("[통합스케줄러] 처리 실패: ${e.message}", e)
        }
    }

    /**
     * 이벤트 배치 처리
     */
    private fun processEventBatch(): Int {
        logger.info("[이벤트배치] 배치 처리 시작")

        var totalProcessed = 0
        val currentHour = getCurrentHour()
        val targetHours = (1..2).map { currentHour - it } // 1~2시간 전 데이터 처리

        targetHours.forEach { hour ->
            val processed = processEventsForHour(hour)
            totalProcessed += processed
        }

        logger.info("[이벤트배치] 배치 처리 완료: ${totalProcessed}건")
        return totalProcessed
    }

    /**
     * 특정 시간의 이벤트들을 처리
     */
    private fun processEventsForHour(hour: Long): Int {
        val logKey = RedisKeyNames.Stats.eventLogKey(hour)
        val eventLogs = redisTemplate.opsForList().range(logKey, 0, -1) ?: return 0

        if (eventLogs.isEmpty()) {
            logger.debug("[이벤트배치] 시간 $hour: 처리할 이벤트 없음")
            return 0
        }

        logger.debug("[이벤트배치] 시간 $hour: ${eventLogs.size}개 이벤트 처리 시작")

        // 이벤트 파싱 및 집계
        val aggregatedStats = aggregateEvents(eventLogs)

        // 청크 단위로 벌크 업데이트
        val chunks = aggregatedStats.entries.chunked(CHUNK_SIZE)
        chunks.forEach { chunk ->
            bulkUpdateStatistics(chunk)
        }

        // 처리 완료된 로그 삭제
        redisTemplate.delete(logKey)

        logger.debug("[이벤트배치] 시간 $hour: ${eventLogs.size}개 이벤트 처리 완료")
        return eventLogs.size
    }

    /**
     * 이벤트 로그들을 상품별로 집계
     */
    private fun aggregateEvents(eventLogs: List<Any>): Map<Long, AggregatedStats> {
        val statsMap = mutableMapOf<Long, AggregatedStats>()

        eventLogs.forEach { eventLog ->
            try {
                val eventJson = eventLog.toString()
                val eventMap = objectMapper.readValue(eventJson, Map::class.java)

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
                        currentStats.wishCount -= 1
                    }
                }

            } catch (e: Exception) {
                logger.warn("[이벤트배치] 이벤트 파싱 실패: ${e.message}")
            }
        }

        return statsMap
    }

    /**
     * 청크 단위 벌크 업데이트
     */
    @Transactional
    private fun bulkUpdateStatistics(chunk: List<Map.Entry<Long, AggregatedStats>>) {
        try {
            chunk.forEach { (productId, aggregatedStats) ->
                val statistics = productPermanentStatisticsRepository.findByProductId(productId)
                    ?: ProductPermanentStatistics.create(productId)

                // 집계된 값 반영 (가변 객체로 성능 최적화)
                if (aggregatedStats.viewCount > 0) {
                    statistics.addViewCount(aggregatedStats.viewCount)
                }
                if (aggregatedStats.salesCount > 0) {
                    statistics.addSalesCount(aggregatedStats.salesCount)
                }
                if (aggregatedStats.wishCount != 0L) {
                    statistics.updateWishCount(aggregatedStats.wishCount)
                }

                productPermanentStatisticsRepository.save(statistics)
            }

            logger.debug("[이벤트배치] 청크 업데이트 완료: ${chunk.size}건")

        } catch (e: Exception) {
            logger.error("[이벤트배치] 청크 업데이트 실패: ${e.message}", e)
            throw e
        }
    }

    /**
     * 인기 상품 캐시 워밍
     */
    private fun warmPopularProductsCache() {
        logger.info("[캐시워밍] 인기 상품 캐시 갱신 시작")

        try {
            WARM_UP_LIMITS.forEach { limit ->
                warmCacheForLimit(limit)
            }
            logger.info("[캐시워밍] 캐시 갱신 완료: limits=$WARM_UP_LIMITS")
        } catch (e: Exception) {
            logger.error("[캐시워밍] 캐시 갱신 실패: ${e.message}", e)
        }
    }

    /**
     * 특정 limit에 대한 캐시 갱신
     */
    private fun warmCacheForLimit(limit: Int) {
        try {
            // 기존 캐시 삭제
            cacheManager.getCache(CacheNames.PRODUCT_POPULAR)?.evict(limit)

            // 새로운 데이터로 캐시 채우기
            val products = getProductQueryUseCase.getPopularProducts(limit)

            logger.debug("[캐시워밍] 캐시 갱신 완료: limit=$limit, productCount=${products.size}")
        } catch (e: Exception) {
            logger.warn("[캐시워밍] 캐시 갱신 실패: limit=$limit, error=${e.message}")
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