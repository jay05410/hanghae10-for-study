package io.hhplus.ecommerce.common.errorcode

/**
 * 쿠폰 관련 에러 코드
 */
enum class CouponErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    COUPON_SOLD_OUT(
        code = "COUPON001",
        message = "쿠폰이 모두 소진되었습니다. 쿠폰: {couponName}, 남은수량: {remainingQuantity}",
        httpStatus = 409
    ),

    DUPLICATE_COUPON_ISSUE(
        code = "COUPON002",
        message = "이미 발급받은 쿠폰입니다. 쿠폰: {couponName}",
        httpStatus = 409
    ),

    EXPIRED_COUPON(
        code = "COUPON003",
        message = "만료된 쿠폰입니다. 만료일: {expiredDate}",
        httpStatus = 400
    ),

    ALREADY_USED_COUPON(
        code = "COUPON004",
        message = "이미 사용된 쿠폰입니다. 사용자 쿠폰 ID: {userCouponId}",
        httpStatus = 409
    ),

    MINIMUM_ORDER_AMOUNT_NOT_MET(
        code = "COUPON005",
        message = "최소 주문 금액을 충족하지 못했습니다. 최소금액: {minAmount}, 주문금액: {orderAmount}",
        httpStatus = 400
    ),

    COUPON_NOT_FOUND(
        code = "COUPON006",
        message = "쿠폰을 찾을 수 없습니다. ID: {couponId}",
        httpStatus = 404
    );
}