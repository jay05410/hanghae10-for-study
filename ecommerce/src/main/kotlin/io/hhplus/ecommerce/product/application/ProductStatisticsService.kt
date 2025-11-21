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
     * 인기 상품 조회 (Redis 실시간 랭킹 기반) - 판매량순
     *
     * @param limit 조회할 상품 수
     * @return 판매량순 인기 상품 통계 목록
     */
    fun getPopularProducts(limit: Int): List<ProductStatistics> {
        return getPopularProducts(limit, "sales")
    }

    /**
     * 인기 상품 조회 (Redis 실시간 랭킹 기반) - 정렬 기준 선택 가능
     *
     * @param limit 조회할 상품 수
     * @param sortBy 정렬 기준 ("sales": 판매량순, "views": 조회수순, "hot": 종합인기순)
     * @return 인기 상품 통계 목록
     */
    fun getPopularProducts(limit: Int, sortBy: String): List<ProductStatistics> {
        // Redis에서 인기 상품 ID 목록 조회
        val popularProductIds = when (sortBy) {
            "views" -> productStatisticsCacheService.getPopularProductsByViews(limit)
            "hot" -> productStatisticsCacheService.getPopularProductsByHot(limit)
            else -> productStatisticsCacheService.getPopularProductsBySales(limit)
        }

        // 각 상품의 통계 정보 조회
        return popularProductIds.mapNotNull { productId ->
            getProductStatistics(productId)
        }
    }

    /**
     * 상품 통계 조회 (Redis + DB 합산)
     *
     * @param productId 상품 ID
     * @return Redis와 DB를 합산한 최신 통계 (상품이 존재하지 않고 Redis에도 데이터가 없으면 null)
     */
    fun getProductStatistics(productId: Long): ProductStatistics? {
        val dbStats = productStatisticsRepository.findByProductId(productId)
        val redisViewCount = productStatisticsCacheService.getViewCount(productId)
        val redisSalesCount = productStatisticsCacheService.getSalesCount(productId)

        // DB와 Redis 모두에 데이터가 없으면 null 반환
        if (dbStats == null && redisViewCount == 0L && redisSalesCount == 0L) {
            return null
        }

        // DB에 데이터가 없으면 새로 생성 (Redis에 데이터가 있는 경우)
        val baseStats = dbStats ?: ProductStatistics.create(productId)

        return ProductStatistics(
            id = baseStats.id,
            productId = productId,
            viewCount = baseStats.viewCount + redisViewCount,
            salesCount = baseStats.salesCount + redisSalesCount,
            version = baseStats.version
        )
    }
}