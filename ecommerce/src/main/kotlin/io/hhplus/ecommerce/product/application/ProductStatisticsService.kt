package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import org.springframework.stereotype.Service

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

    fun incrementViewCount(productId: Long, userId: Long): ProductStatistics {
        val statistics = productStatisticsRepository.findByProductIdWithLock(productId)
            ?: createStatistics(productId, userId)

        statistics.incrementViewCount(userId)
        return productStatisticsRepository.save(statistics)
    }

    fun incrementSalesCount(productId: Long, quantity: Int, userId: Long): ProductStatistics {
        val statistics = productStatisticsRepository.findByProductIdWithLock(productId)
            ?: createStatistics(productId, userId)

        statistics.incrementSalesCount(quantity, userId)
        return productStatisticsRepository.save(statistics)
    }

    fun getPopularProducts(limit: Int): List<ProductStatistics> {
        return productStatisticsRepository.findTopPopularProducts(limit)
    }

    fun getProductStatistics(productId: Long): ProductStatistics? {
        return productStatisticsRepository.findByProductId(productId)
    }

    private fun createStatistics(productId: Long, createdBy: Long): ProductStatistics {
        val statistics = ProductStatistics.create(productId, createdBy)
        return productStatisticsRepository.save(statistics)
    }
}