package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 통계 Redis → DB 동기화 스케줄러
 *
 * 역할:
 * - Redis에 쌓인 조회수/판매량을 주기적으로 DB에 동기화
 * - 데이터 영속성 보장
 * - Redis 메모리 관리
 */
@Component
class ProductStatisticsSyncScheduler(
    private val productStatisticsCacheService: ProductStatisticsCacheService,
    private val productStatisticsRepository: ProductStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 1분마다 Redis → DB 동기화
     */
    @Scheduled(fixedDelay = 60000)
    fun syncStatisticsToDatabase() {
        logger.info("[ProductStatistics] Redis → DB 동기화 시작")

        var syncedViewCount = 0
        var syncedSalesCount = 0

        try {
            // 조회수 동기화
            val viewCountKeys = productStatisticsCacheService.getAllViewCountKeys()
            logger.debug("[ProductStatistics] 조회수 동기화 대상: ${viewCountKeys.size}개")

            viewCountKeys.forEach { key ->
                val productId = extractProductId(key) ?: return@forEach
                val count = productStatisticsCacheService.getAndClearViewCount(productId)

                if (count > 0) {
                    syncViewCount(productId, count)
                    syncedViewCount++
                }
            }

            // 판매량 동기화
            val salesCountKeys = productStatisticsCacheService.getAllSalesCountKeys()
            logger.debug("[ProductStatistics] 판매량 동기화 대상: ${salesCountKeys.size}개")

            salesCountKeys.forEach { key ->
                val productId = extractProductId(key) ?: return@forEach
                val count = productStatisticsCacheService.getAndClearSalesCount(productId)

                if (count > 0) {
                    syncSalesCount(productId, count)
                    syncedSalesCount++
                }
            }

            logger.info("[ProductStatistics] Redis → DB 동기화 완료: 조회수 ${syncedViewCount}건, 판매량 ${syncedSalesCount}건")

        } catch (e: Exception) {
            logger.error("[ProductStatistics] Redis → DB 동기화 실패: ${e.message}", e)
        }
    }

    @Transactional
    protected fun syncViewCount(productId: Long, count: Long) {
        try {
            var statistics = productStatisticsRepository.findByProductId(productId)
            if (statistics == null) {
                statistics = ProductStatistics.create(productId)
                productStatisticsRepository.save(statistics)
            }

            repeat(count.toInt()) {
                statistics.incrementViewCount()
            }
            productStatisticsRepository.save(statistics)

            logger.debug("[ProductStatistics] 조회수 동기화 성공: productId=$productId, count=$count")

        } catch (e: Exception) {
            logger.error("[ProductStatistics] 조회수 동기화 실패: productId=$productId, count=$count, error=${e.message}")
            throw e
        }
    }

    @Transactional
    protected fun syncSalesCount(productId: Long, count: Long) {
        try {
            var statistics = productStatisticsRepository.findByProductId(productId)
            if (statistics == null) {
                statistics = ProductStatistics.create(productId)
                productStatisticsRepository.save(statistics)
            }

            statistics.incrementSalesCount(count.toInt())
            productStatisticsRepository.save(statistics)

            logger.debug("[ProductStatistics] 판매량 동기화 성공: productId=$productId, count=$count")

        } catch (e: Exception) {
            logger.error("[ProductStatistics] 판매량 동기화 실패: productId=$productId, count=$count, error=${e.message}")
            throw e
        }
    }

    /**
     * Redis 키에서 productId 추출
     *
     * @param key Redis 키 (예: "product:view:123")
     * @return 추출된 productId (실패 시 null)
     */
    private fun extractProductId(key: String): Long? {
        return try {
            key.substringAfterLast(":").toLongOrNull()
        } catch (e: Exception) {
            logger.warn("[ProductStatistics] productId 추출 실패: key=$key")
            null
        }
    }
}
