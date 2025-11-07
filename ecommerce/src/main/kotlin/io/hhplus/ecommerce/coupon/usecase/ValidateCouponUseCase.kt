package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import org.springframework.stereotype.Component

/**
 * 쿠폰 사용 검증 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 쿠폰 사용 자격 및 사용 조건 검증
 * - 쿠폰 적용 전 할인 금액 미리 계산
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 쿠폰 유효성 및 사용 자격 검증
 * - 주문 금액 기반 할인 금액 사전 계산
 * - 읽기 전용 검증 로직 처리
 */
@Component
class ValidateCouponUseCase(
    private val couponService: CouponService
) {

    /**
     * 쿠폰 사용 가능성을 검증하고 예상 할인 금액을 계산한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 사용 검증 요청 데이터
     * @return 검증 성공 시 계산된 할인 금액
     * @throws IllegalArgumentException 쿠폰 사용 조건을 만족하지 않는 경우
     */
    fun execute(userId: Long, request: UseCouponRequest): Long {
        return couponService.validateCouponUsage(
            userId = userId,
            userCouponId = request.userCouponId,
            orderAmount = request.orderAmount
        )
    }
}