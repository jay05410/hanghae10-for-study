package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.EventBasedStatisticsService
import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.product.domain.calculator.PopularityCalculator
import io.hhplus.ecommerce.product.domain.entity.Product
import org.springframework.stereotype.Component

/**
 * 상품 통계 기반 조회 UseCase
 *
 * 역할:
 * - 통계 데이터를 활용한 상품 정렬 및 조회
 * - 인기순, 조회순, 찜순, 판매순 등 통계 기반 비즈니스 로직
 * - 실시간 통계와 상품 데이터 결합
 *
 * 분리 이유:
 * - GetProductQueryUseCase는 순수 상품 조회만 담당
 * - 통계 관련 복잡한 로직을 별도 관리
 * - 단일 책임 원칙 준수
 */
@Component
class ProductStatisticsQueryUseCase(
    private val productQueryService: ProductQueryService,
    private val eventBasedStatisticsService: EventBasedStatisticsService
) {

    /**
     * 비즈니스 로직: 다양한 정렬 기준으로 상품 목록 조회
     *
     * 정렬 기준별 비즈니스 로직:
     * - 인기순: 판매량 40% + 조회수 30% + 찜 30% 가중치 적용
     * - 조회순: 최근 10분간 실시간 조회수 기준
     * - 찜순: 현재 찜 개수 기준
     * - 판매순: 누적 판매량 기준
     */
    fun getProductsBySortCriteria(sortBy: ProductSortCriteria, limit: Int = 20): List<Product> {
        return when (sortBy) {
            ProductSortCriteria.POPULAR -> {
                // 인기순: 실시간 통계 기반 인기 상품 (이미 Redis에서 계산됨)
                getPopularProducts(limit)
            }

            ProductSortCriteria.MOST_VIEWED -> {
                // 조회순: 최근 10분간 조회수가 높은 상품들
                getMostViewedProducts(limit)
            }

            ProductSortCriteria.MOST_WISHED -> {
                // 찜순: 현재 찜 개수가 많은 상품들
                getMostWishedProducts(limit)
            }

            ProductSortCriteria.BEST_SELLING -> {
                // 판매순: 누적 판매량이 높은 상품들
                getBestSellingProducts(limit)
            }
        }
    }

    /**
     * 특정 카테고리에서 정렬 기준에 따른 상품 목록 조회
     * 카테고리 + 정렬 기준 조합의 비즈니스 로직
     */
    fun getProductsByCategoryAndSortCriteria(
        categoryId: Long,
        sortBy: ProductSortCriteria,
        limit: Int = 20
    ): List<Product> {
        // 1. 해당 카테고리의 활성 상품 조회
        val categoryProducts = productQueryService.getActiveProductsByCategory(categoryId)

        // 2. 정렬 기준에 따른 필터링 및 정렬
        return when (sortBy) {
            ProductSortCriteria.POPULAR -> {
                categoryProducts.map { product ->
                    val (viewCount, salesCount, wishCount) = eventBasedStatisticsService.getRealTimeStats(product.id)
                    product to PopularityCalculator.calculateScore(salesCount, viewCount, wishCount)
                }.sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
            }

            ProductSortCriteria.MOST_VIEWED -> {
                categoryProducts.map { product ->
                    val viewCount = eventBasedStatisticsService.getLast10MinuteViews(product.id)
                    product to viewCount
                }.sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
            }

            ProductSortCriteria.MOST_WISHED -> {
                categoryProducts.map { product ->
                    val (_, _, wishCount) = eventBasedStatisticsService.getRealTimeStats(product.id)
                    product to wishCount
                }.sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
            }

            ProductSortCriteria.BEST_SELLING -> {
                categoryProducts.map { product ->
                    val (_, salesCount, _) = eventBasedStatisticsService.getRealTimeStats(product.id)
                    product to salesCount
                }.sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
            }
        }
    }

    /**
     * 실시간 인기 상품 조회 (Redis 기반)
     */
    private fun getPopularProducts(limit: Int): List<Product> {
        val popularProductIds = eventBasedStatisticsService.getRealTimePopularProducts(limit)

        return popularProductIds.mapNotNull { (productId, _) ->
            try {
                productQueryService.getProduct(productId)
            } catch (e: Exception) {
                null // 상품이 삭제된 경우 무시
            }
        }
    }

    /**
     * 최근 조회수가 높은 상품 목록 (최근 10분 기준)
     */
    private fun getMostViewedProducts(limit: Int): List<Product> {
        val allProductsWithViews = mutableListOf<Pair<Long, Long>>()

        // TODO: 성능 최적화 필요 - Redis Sorted Set으로 관리 고려
        // 현재는 샘플링 방식으로 상위 1000개 상품만 체크
        for (productId in 1L..1000L) {
            try {
                val viewCount = eventBasedStatisticsService.getLast10MinuteViews(productId)
                if (viewCount > 0) {
                    allProductsWithViews.add(productId to viewCount)
                }
            } catch (e: Exception) {
                continue
            }
        }

        return allProductsWithViews
            .sortedByDescending { it.second }
            .take(limit)
            .mapNotNull { (productId, _) ->
                try {
                    productQueryService.getProduct(productId)
                } catch (e: Exception) {
                    null
                }
            }
    }

    /**
     * 찜이 많은 상품 목록
     */
    private fun getMostWishedProducts(limit: Int): List<Product> {
        val allProductsWithWishes = mutableListOf<Pair<Long, Long>>()

        for (productId in 1L..1000L) {
            try {
                val (_, _, wishCount) = eventBasedStatisticsService.getRealTimeStats(productId)
                if (wishCount > 0) {
                    allProductsWithWishes.add(productId to wishCount)
                }
            } catch (e: Exception) {
                continue
            }
        }

        return allProductsWithWishes
            .sortedByDescending { it.second }
            .take(limit)
            .mapNotNull { (productId, _) ->
                try {
                    productQueryService.getProduct(productId)
                } catch (e: Exception) {
                    null
                }
            }
    }

    /**
     * 판매량이 높은 상품 목록
     */
    private fun getBestSellingProducts(limit: Int): List<Product> {
        val allProductsWithSales = mutableListOf<Pair<Long, Long>>()

        for (productId in 1L..1000L) {
            try {
                val (_, salesCount, _) = eventBasedStatisticsService.getRealTimeStats(productId)
                if (salesCount > 0) {
                    allProductsWithSales.add(productId to salesCount)
                }
            } catch (e: Exception) {
                continue
            }
        }

        return allProductsWithSales
            .sortedByDescending { it.second }
            .take(limit)
            .mapNotNull { (productId, _) ->
                try {
                    productQueryService.getProduct(productId)
                } catch (e: Exception) {
                    null
                }
            }
    }
}

/**
 * 상품 정렬 기준 열거형
 * 비즈니스 요구사항에 따른 정렬 옵션 정의
 */
enum class ProductSortCriteria {
    POPULAR,      // 인기순 (종합 점수)
    MOST_VIEWED,  // 조회순 (최근 10분)
    MOST_WISHED,  // 찜순 (현재 찜 개수)
    BEST_SELLING  // 판매순 (누적 판매량)
}