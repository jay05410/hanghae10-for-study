package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductStatisticsService
import org.springframework.stereotype.Component

/**
 * 상품 조회수 증가 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 조회시 조회수 증가 비즈니스 플로우 수행
 * - 상품 인기도 통계 데이터 업데이트
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 상품 조회수 중복 증가 방지 로직
 * - 상품 통계 데이터 업데이트 트랜잭션 관리
 * - 비동기적 통계 데이터 처리
 */
@Component
class IncrementProductViewUseCase(
    private val productStatisticsService: ProductStatisticsService
) {

    /**
     * 상품 조회시 해당 상품의 조회수를 증가시킨다
     *
     * @param productId 조회된 상품 ID
     * @param userId 상품을 조회한 사용자 ID
     */
    fun execute(productId: Long, userId: Long) {
        productStatisticsService.incrementViewCount(productId, userId)
    }
}