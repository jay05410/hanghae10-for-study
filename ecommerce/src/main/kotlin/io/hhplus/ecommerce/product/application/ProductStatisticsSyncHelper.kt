package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 통계 동기화 Helper
 *
 * 역할:
 * - 트랜잭션 관리 및 DB 동기화 처리
 * - Self-Invocation 문제 해결을 위해 Scheduler와 분리
 * - 각 상품별 독립적인 트랜잭션 보장
 */
@Component
class ProductStatisticsSyncHelper(
    private val productStatisticsRepository: ProductStatisticsRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 조회수 동기화 (트랜잭션 적용)
     *
     * @param productId 상품 ID
     * @param count 증가할 조회수
     */
    @Transactional
    fun syncViewCount(productId: Long, count: Long) {
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

    /**
     * 판매량 동기화 (트랜잭션 적용)
     *
     * @param productId 상품 ID
     * @param count 증가할 판매량
     */
    @Transactional
    fun syncSalesCount(productId: Long, count: Long) {
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
}
