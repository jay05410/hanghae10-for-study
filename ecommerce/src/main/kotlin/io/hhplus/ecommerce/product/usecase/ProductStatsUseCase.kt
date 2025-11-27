package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.EventBasedStatisticsService
import io.hhplus.ecommerce.product.domain.vo.ProductStatsVO
import org.springframework.stereotype.Component

/**
 * 상품 통계 UseCase - 이벤트 기반 단방향 통계
 *
 * 역할:
 * - 상품 관련 통계 이벤트 발생 및 처리
 * - Write-back 없는 단방향 이벤트 플로우
 *
 * 특징:
 * - 이벤트 발생 → Redis 로그 저장 → 배치 집계 → DB 벨크 업데이트
 * - 실시간 인기상품 조회: Redis 로그에서 직접 계산
 */
@Component
class ProductStatsUseCase(
    private val eventBasedStatisticsService: EventBasedStatisticsService
) {

    /**
     * 상품 조회 이벤트 발생
     *
     * @param productId 조회된 상품 ID
     * @param userId 상품을 조회한 사용자 ID
     * @return 최근 10분간 실시간 조회수
     */
    fun incrementViewCount(productId: Long, userId: Long): Long {
        return eventBasedStatisticsService.recordViewEvent(productId, userId)
    }

    /**
     * 상품 판매 이벤트 발생
     *
     * @param productId 상품 ID
     * @param quantity 판매 수량
     * @param orderId 주문 ID
     * @return 업데이트된 실시간 통계 정보
     */
    fun incrementSalesCount(productId: Long, quantity: Int, orderId: Long): ProductStatsVO {
        val totalSalesCount = eventBasedStatisticsService.recordSalesEvent(productId, quantity, orderId)
        val (viewCount, salesCount, wishCount) = eventBasedStatisticsService.getRealTimeStats(productId)

        return ProductStatsVO.create(
            productId = productId,
            viewCount = viewCount,
            salesCount = salesCount,
            hotScore = calculateHotScore(viewCount, salesCount, wishCount)
        )
    }

    /**
     * 상품 찜 이벤트 발생
     *
     * @param productId 상품 ID
     * @param userId 사용자 ID
     * @return 업데이트된 찜 개수
     */
    fun addWish(productId: Long, userId: Long): Long {
        return eventBasedStatisticsService.recordWishEvent(productId, userId)
    }

    /**
     * 상품 찜 해제 이벤트 발생
     *
     * @param productId 상품 ID
     * @param userId 사용자 ID
     * @return 업데이트된 찜 개수
     */
    fun removeWish(productId: Long, userId: Long): Long {
        return eventBasedStatisticsService.recordUnwishEvent(productId, userId)
    }

    /**
     * 인기도 점수 계산
     */
    private fun calculateHotScore(viewCount: Long, salesCount: Long, wishCount: Long): Double {
        return salesCount * 0.4 + viewCount * 0.3 + wishCount * 0.3
    }
}