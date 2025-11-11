package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import org.springframework.stereotype.Component

/**
 * 쿠폰 사용 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 쿠폰 실제 사용 처리 비즈니스 플로우 수행
 * - 주문과 연계된 쿠폰 적용 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 쿠폰 사용 자격 재검증
 * - 주문 금액 기반 할인 계산 및 적용
 * - 쿠폰 사용 상태 변경 트랜잭션 관리
 */
@Component
class ApplyCouponUseCase(
    private val couponService: CouponService
) {

    /**
     * 주문에 쿠폰을 실제로 적용하고 할인 금액을 계산한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 사용 요청 데이터
     * @param orderId 쿠폰이 적용될 주문 ID
     * @return 계산된 할인 금액
     * @throws IllegalArgumentException 쿠폰 사용 조건을 만족하지 않는 경우
     * @throws RuntimeException 쿠폰 사용 처리에 실패한 경우
     */
    fun execute(userId: Long, request: UseCouponRequest, orderId: Long): Long {
        return couponService.applyCoupon(
            userId = userId,
            userCouponId = request.userCouponId,
            orderId = orderId,
            orderAmount = request.orderAmount
        )
    }
}