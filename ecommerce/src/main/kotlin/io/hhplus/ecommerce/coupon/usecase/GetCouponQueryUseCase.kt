package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import org.springframework.stereotype.Component

/**
 * 쿠폰 조회 통합 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 쿠폰 관련 다양한 조회 작업 통합 처리
 * - 사용자별 쿠폰 정보 조회 및 비즈니스 로직 수행
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 쿠폰 조회 사용 사례 통합 처리
 * - 사용자 쿠폰 데이터 반환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetCouponQueryUseCase(
    private val couponService: CouponService
) {

    /**
     * 현재 발급 가능한 쿠폰 목록을 조회한다
     *
     * @return 발급 가능한 쿠폰 목록
     */
    fun getAvailableCoupons(): List<Coupon> {
        return couponService.getAvailableCoupons()
    }

    /**
     * 사용자 쿠폰 조회
     *
     * @param userId 사용자 ID
     * @param onlyAvailable true면 사용 가능한 쿠폰만, false면 전체
     */
    fun getUserCoupons(userId: Long, onlyAvailable: Boolean = false): List<UserCoupon> {
        return if (onlyAvailable) {
            couponService.getUserCoupons(userId, io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus.ISSUED)
        } else {
            couponService.getUserCoupons(userId)
        }
    }
}