package io.hhplus.ecommerce.common.exception.coupon

import io.hhplus.ecommerce.common.errorcode.CouponErrorCode
import io.hhplus.ecommerce.common.exception.BusinessException
import org.slf4j.event.Level

/**
 * 쿠폰 관련 예외 클래스
 *
 * 쿠폰 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 CouponErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class CouponException(
    errorCode: CouponErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 쿠폰 품절 예외
     */
    class CouponSoldOut(couponName: String, remainingQuantity: Int) : CouponException(
        errorCode = CouponErrorCode.COUPON_SOLD_OUT,
        message = CouponErrorCode.COUPON_SOLD_OUT.withParams(
            "couponName" to couponName,
            "remainingQuantity" to remainingQuantity
        ),
        data = mapOf(
            "couponName" to couponName,
            "remainingQuantity" to remainingQuantity
        )
    )

    /**
     * 중복 쿠폰 발급 예외
     */
    class DuplicateCouponIssue(couponName: String) : CouponException(
        errorCode = CouponErrorCode.DUPLICATE_COUPON_ISSUE,
        message = CouponErrorCode.DUPLICATE_COUPON_ISSUE.withParams("couponName" to couponName),
        data = mapOf("couponName" to couponName)
    )

    /**
     * 만료된 쿠폰 예외
     */
    class ExpiredCoupon(expiredDate: String) : CouponException(
        errorCode = CouponErrorCode.EXPIRED_COUPON,
        message = CouponErrorCode.EXPIRED_COUPON.withParams("expiredDate" to expiredDate),
        data = mapOf("expiredDate" to expiredDate)
    )

    /**
     * 이미 사용된 쿠폰 예외
     */
    class AlreadyUsedCoupon(userCouponId: Long) : CouponException(
        errorCode = CouponErrorCode.ALREADY_USED_COUPON,
        message = CouponErrorCode.ALREADY_USED_COUPON.withParams("userCouponId" to userCouponId),
        data = mapOf("userCouponId" to userCouponId)
    )

    /**
     * 최소 주문 금액 미달 예외
     */
    class MinimumOrderAmountNotMet(minAmount: Long, orderAmount: Long) : CouponException(
        errorCode = CouponErrorCode.MINIMUM_ORDER_AMOUNT_NOT_MET,
        message = CouponErrorCode.MINIMUM_ORDER_AMOUNT_NOT_MET.withParams(
            "minAmount" to minAmount,
            "orderAmount" to orderAmount
        ),
        data = mapOf(
            "minAmount" to minAmount,
            "orderAmount" to orderAmount
        )
    )

    /**
     * 쿠폰을 찾을 수 없음 예외
     */
    class CouponNotFound(couponId: Long) : CouponException(
        errorCode = CouponErrorCode.COUPON_NOT_FOUND,
        message = CouponErrorCode.COUPON_NOT_FOUND.withParams("couponId" to couponId),
        data = mapOf("couponId" to couponId)
    )
}