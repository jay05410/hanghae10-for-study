package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.application.EventBasedStatisticsService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.vo.ProductStatsVO
import org.springframework.stereotype.Component

/**
 * 상품 조회 통합 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 관련 다양한 조회 작업 통합 처리
 * - 상품 정보 조회 및 비즈니스 로직 수행
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 상품 조회 사용 사례 통합 처리
 * - 상품 데이터 반환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetProductQueryUseCase(
    private val productService: ProductService,
    private val eventBasedStatisticsService: EventBasedStatisticsService
) {

    /**
     * 상품 목록을 페이지 단위로 조회한다
     *
     * @param page 조회할 페이지 번호
     * @return 해당 페이지의 상품 목록
     */
    fun getProducts(page: Int): List<Product> {
        return productService.getProducts(page)
    }

    /**
     * 상품 ID로 특정 상품을 조회한다
     *
     * @param productId 조회할 상품 ID
     * @return 상품 정보
     * @throws IllegalArgumentException 상품을 찾을 수 없는 경우
     */
    fun getProduct(productId: Long): Product {
        return productService.getProduct(productId)
    }

    /**
     * 특정 카테고리에 속하는 상품 목록을 조회한다
     *
     * @param categoryId 카테고리 ID
     * @return 해당 카테고리에 속하는 상품 목록
     */
    fun getProductsByCategory(categoryId: Long): List<Product> {
        return productService.getProductsByCategory(categoryId)
    }

    /**
     * 상품 통계를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 실시간 통계 정보
     */
    fun getProductStatistics(productId: Long): ProductStatsVO {
        val (viewCount, salesCount, wishCount) = eventBasedStatisticsService.getRealTimeStats(productId)
        return ProductStatsVO.create(
            productId = productId,
            viewCount = viewCount,
            salesCount = salesCount,
            hotScore = salesCount * 0.4 + viewCount * 0.3 + wishCount * 0.3
        )
    }

    /**
     * 인기 상품 통계 목록을 순위순으로 조회한다
     *
     * @param limit 조회할 인기 상품 수 (기본 10개)
     * @return 최근 10분 실시간 인기 상품 목록
     */
    fun getPopularStatistics(limit: Int = 10): List<ProductStatsVO> {
        return eventBasedStatisticsService.getRealTimePopularProducts(limit).map { (productId, viewCount) ->
            val (_, salesCount, wishCount) = eventBasedStatisticsService.getRealTimeStats(productId)
            ProductStatsVO.create(
                productId = productId,
                viewCount = viewCount,
                salesCount = salesCount,
                hotScore = salesCount * 0.4 + viewCount * 0.3 + wishCount * 0.3
            )
        }
    }

    /**
     * 인기 상품 목록을 순위순으로 조회한다
     *
     * @param limit 조회할 인기 상품 수 (기본 10개)
     * @return 최근 10분 실시간 인기 상품 목록
     */
    fun getPopularProducts(limit: Int = 10): List<Product> {
        val popularProductIds = eventBasedStatisticsService.getRealTimePopularProducts(limit)

        return popularProductIds.map { (productId, _) ->
            productService.getProduct(productId)
        }
    }
}