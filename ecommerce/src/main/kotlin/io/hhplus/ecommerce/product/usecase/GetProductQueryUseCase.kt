package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.application.EventBasedStatisticsService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.vo.ProductStatsVO
import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.common.response.Cursor
import org.springframework.cache.annotation.Cacheable
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
     * 상품 목록을 커서 기반 페이징으로 조회한다
     *
     * @param lastId 마지막 상품 ID (커서)
     * @param size 조회할 상품 수
     * @return 커서 기반 상품 목록
     */
    fun getProducts(lastId: Long?, size: Int): Cursor<Product> {
        return productService.getProductsWithCursor(lastId, size)
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
     * 특정 카테고리에 속하는 상품 목록을 커서 기반 페이징으로 조회한다
     *
     * @param categoryId 카테고리 ID
     * @param lastId 마지막 상품 ID (커서)
     * @param size 조회할 상품 수
     * @return 해당 카테고리에 속하는 커서 기반 상품 목록
     */
    fun getProductsByCategory(categoryId: Long, lastId: Long?, size: Int): Cursor<Product> {
        return productService.getProductsByCategoryWithCursor(categoryId, lastId, size)
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
     * 인기 상품 목록을 순위순으로 조회한다 - 2단계 캐싱 적용
     *
     * 1단계: Redis 통계 로그에서 실시간 인기 순위 계산 (EventBasedStatisticsService)
     * 2단계: 최종 결과를 Redis 캐시에 저장 (동시 조회 트래픽 대응)
     * TTL: 30초 (실시간성 중요, 대량 트래픽 처리)
     *
     * @param limit 조회할 인기 상품 수 (기본 10개)
     * @return 최근 10분 실시간 인기 상품 목록
     */
    @Cacheable(value = [CacheNames.PRODUCT_POPULAR], key = "#limit", cacheManager = "redisCacheManager")
    fun getPopularProducts(limit: Int = 10): List<Product> {
        // 1단계: Redis 통계에서 인기 상품 ID 조회 (복잡한 계산)
        val popularProductIds = eventBasedStatisticsService.getRealTimePopularProducts(limit)

        // 2단계: 상품 상세 정보 조회 (각각 로컬 캐시 적용됨)
        return popularProductIds.map { (productId, _) ->
            productService.getProduct(productId)
        }
    }
}