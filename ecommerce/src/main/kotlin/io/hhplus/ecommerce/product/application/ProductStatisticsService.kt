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
    private val productStatisticsRepository: ProductStatisticsRepository,
    private val productStatisticsCacheService: ProductStatisticsCacheService
) {

    /**
     * 조회수 증가 (Redis 캐시 사용)
     *
     * @param productId 상품 ID
     * @return 증가된 Redis 조회수
     */
    fun incrementViewCount(productId: Long): Long {
        return productStatisticsCacheService.incrementViewCount(productId)
    }

    /**
     * 판매량 증가 (Redis 캐시 사용)
     *
     * @param productId 상품 ID
     * @param quantity 판매 수량
     * @return 증가된 Redis 판매량
     */
    fun incrementSalesCount(productId: Long, quantity: Int): Long {
        return productStatisticsCacheService.incrementSalesCount(productId, quantity)
    }

    /**
     * 인기 상품 조회 (DB 기준)
     *
     * 주의: 실시간 랭킹이 필요하면 별도 Redis Sorted Set 구현 필요
     */
    fun getPopularProducts(limit: Int): List<ProductStatistics> {
        return productStatisticsRepository.findTopPopularProducts(limit)
    }

    /**
     * 상품 통계 조회 (Redis + DB 합산)
     *
     * @param productId 상품 ID
     * @return Redis와 DB를 합산한 최신 통계
     */
    fun getProductStatistics(productId: Long): ProductStatistics {
        val dbStats = productStatisticsRepository.findByProductId(productId)
            ?: ProductStatistics.create(productId)

        val redisViewCount = productStatisticsCacheService.getViewCount(productId)
        val redisSalesCount = productStatisticsCacheService.getSalesCount(productId)

        return ProductStatistics(
            id = dbStats.id,
            productId = productId,
            viewCount = dbStats.viewCount + redisViewCount,
            salesCount = dbStats.salesCount + redisSalesCount,
            version = dbStats.version
        )
    }
}