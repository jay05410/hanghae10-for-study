package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 쿠폰 발급 비즈니스 플로우 수행
 * - 쿠폰 발급 자격 검증 및 발급 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 쿠폰 발급 자격 및 수량 제한 검증
 * - 사용자 쿠폰 생성 트랜잭션 관리
 * - 쿠폰 발급 후 관련 비즈니스 규칙 적용
 */
@Component
class IssueCouponUseCase(
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
    fun execute(userId: Long, request: IssueCouponRequest): UserCoupon {
        return couponService.issueCoupon(
            userId = userId,
            couponId = request.couponId
        )
    }
}