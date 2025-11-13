package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import org.springframework.stereotype.Component

/**
 * 쿠폰 명령 UseCase
 *
 * 역할:
 * - 모든 쿠폰 변경 작업을 통합 관리
 * - 쿠폰 발급, 사용 기능 제공
 *
 * 책임:
 * - 쿠폰 발급/사용 요청 검증 및 실행
 * - 쿠폰 데이터 무결성 보장
 */
@Component
class CouponCommandUseCase(
    private val couponService: CouponService
) {

    /**
     * 사용자에게 지정된 쿠폰을 발급한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 발급 요청 데이터
     * @return 사용자에게 발급된 쿠폰
     * @throws IllegalArgumentException 발급 불가능한 쿠폰이거나 유효하지 않은 사용자인 경우
     * @throws RuntimeException 쿠폰 발급 수량 초과 또는 발급 처리 실패인 경우
     */
    fun issueCoupon(userId: Long, request: IssueCouponRequest): UserCoupon {
        return couponService.issueCoupon(
            userId = userId,
            couponId = request.couponId
        )
    }

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
    fun applyCoupon(userId: Long, request: UseCouponRequest, orderId: Long): Long {
        return couponService.applyCoupon(
            userId = userId,
            userCouponId = request.userCouponId,
            orderId = orderId,
            orderAmount = request.orderAmount
        )
    }
}