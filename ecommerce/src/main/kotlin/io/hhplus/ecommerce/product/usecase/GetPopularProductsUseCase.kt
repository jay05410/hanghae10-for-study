package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.domain.entity.Product
import org.springframework.stereotype.Component

/**
 * 인기 상품 조회 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 인기 상품 순위 기반 데이터 조회 및 제공
 * - 상품 통계 데이터와 상품 정보 연결
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 인기 상품 통계 데이터 조회 및 처리
 * - 통계 기반 상품 정보 변환 및 전달
 * - 읽기 전용 인기도 데이터 처리
 */
@Component
class GetPopularProductsUseCase(
    private val productStatisticsService: ProductStatisticsService,
    private val productService: ProductService
) {

    /**
     * 인기 상품 목록을 순위순으로 조회한다
     *
     * @param limit 조회할 인기 상품 수 (기본 10개)
     * @return 인기 순위에 따른 상품 목록
     */
    fun execute(limit: Int = 10): List<Product> {
        val popularStatistics = productStatisticsService.getPopularProducts(limit)

        return popularStatistics.map { statistics ->
            productService.getProduct(statistics.productId)
        }
    }
}