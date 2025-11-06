package io.hhplus.ecommerce.domain.coupon.validator

import io.hhplus.ecommerce.common.exception.coupon.CouponException
import io.hhplus.ecommerce.domain.coupon.entity.Coupon
import io.hhplus.ecommerce.domain.coupon.entity.UserCoupon
import java.time.LocalDateTime

/**
 * 쿠폰 검증 유틸리티
 *
 * 책임: DB 호출 없는 순수 검증 로직만 포함
 */
object CouponValidator {

    /**
     * 쿠폰 사용 가능 여부 검증
     *
     * @param coupon 쿠폰 정보
     * @param orderAmount 주문 금액
     * @throws CouponException 사용 불가능한 쿠폰일 시
     */
    fun validateCouponUsage(coupon: Coupon, orderAmount: Long) {
        // 활성 상태 확인
        require(coupon.isActive) { "비활성 쿠폰입니다" }

        // 유효 기간 확인
        if (!coupon.isWithinValidPeriod()) {
            throw CouponException.ExpiredCoupon(coupon.validTo.toString())
        }

        // 최소 주문 금액 확인
        if (!coupon.isValidForUse(orderAmount)) {
            throw CouponException.MinimumOrderAmountNotMet(coupon.minimumOrderAmount, orderAmount)
        }
    }

    /**
     * 사용자 쿠폰 사용 가능 여부 검증
     *
     * @param userCoupon 사용자 쿠폰
     * @throws CouponException.AlreadyUsedCoupon 이미 사용된 쿠폰일 시
     */
    fun validateUserCouponUsage(userCoupon: UserCoupon) {
        if (!userCoupon.isUsable()) {
            throw CouponException.AlreadyUsedCoupon(userCoupon.id)
        }
    }

    /**
     * 쿠폰 발급 가능 여부 검증
     *
     * @param coupon 쿠폰 정보
     * @throws CouponException 발급 불가능한 쿠폰일 시
     */
    fun validateCouponIssuance(coupon: Coupon) {
        if (!coupon.isAvailableForIssue()) {
            throw CouponException.CouponSoldOut(coupon.name, coupon.getRemainingQuantity())
        }
    }

    /**
     * 중복 쿠폰 발급 검증
     *
     * @param hasExistingCoupon 기존 쿠폰 보유 여부
     * @param couponName 쿠폰명
     * @throws CouponException.DuplicateCouponIssue 중복 발급 시
     */
    fun validateDuplicateIssuance(hasExistingCoupon: Boolean, couponName: String) {
        if (hasExistingCoupon) {
            throw CouponException.DuplicateCouponIssue(couponName)
        }
    }
}