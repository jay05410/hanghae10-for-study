package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 통계 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 통계 데이터 관리 전담
 * - 상품 조회수, 판매량 추적
 * - 인기 상품 분석 및 랭킹 제공
 *
 * 책임:
 * - 상품 조회수 및 판매량 증가 처리
 * - 인기 상품 리스트 제공
 * - 상품별 통계 데이터 조회
 */
@Service
class ProductStatisticsService(
    private val productStatisticsRepository: ProductStatisticsRepository
) {

    @Transactional
    fun incrementViewCount(productId: Long): ProductStatistics {
        val statistics = productStatisticsRepository.findByProductIdWithLock(productId)
            ?: createStatistics(productId)

        statistics.incrementViewCount()
        return productStatisticsRepository.save(statistics)
    }

    @Transactional
    fun incrementSalesCount(productId: Long, quantity: Int): ProductStatistics {
        val statistics = productStatisticsRepository.findByProductIdWithLock(productId)
            ?: createStatistics(productId)

        statistics.incrementSalesCount(quantity)
        return productStatisticsRepository.save(statistics)
    }

    fun getPopularProducts(limit: Int): List<ProductStatistics> {
        return productStatisticsRepository.findTopPopularProducts(limit)
    }

    fun getProductStatistics(productId: Long): ProductStatistics? {
        return productStatisticsRepository.findByProductId(productId)
    }

    private fun createStatistics(productId: Long): ProductStatistics {
        val statistics = ProductStatistics.create(productId)
        return productStatisticsRepository.save(statistics)
    }
}