package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import org.springframework.stereotype.Component

/**
 * 상품 통계 UseCase
 *
 * 역할:
 * - 상품 관련 통계 업데이트 작업을 통합 관리
 * - 조회수 증가, 판매량 증가 기능 제공
 *
 * 책임:
 * - 상품 통계 업데이트 요청 검증 및 실행
 * - 통계 데이터 무결성 보장
 */
@Component
class ProductStatsUseCase(
    private val productStatisticsService: ProductStatisticsService
) {

    /**
     * 상품 조회시 해당 상품의 조회수를 증가시킨다
     *
     * @param productId 조회된 상품 ID
     * @param userId 상품을 조회한 사용자 ID
     */
    fun incrementViewCount(productId: Long, userId: Long) {
        productStatisticsService.incrementViewCount(productId)
    }

    /**
     * 상품 판매량을 증가시킵니다.
     *
     * @param productId 상품 ID
     * @param quantity 판매 수량
     * @param userId 판매 요청자 ID
     * @return 업데이트된 상품 통계 정보 (Redis + DB 합산)
     */
    fun incrementSalesCount(productId: Long, quantity: Int, userId: Long): ProductStatistics {
        productStatisticsService.incrementSalesCount(productId, quantity)
        return productStatisticsService.getProductStatistics(productId)
    }
}