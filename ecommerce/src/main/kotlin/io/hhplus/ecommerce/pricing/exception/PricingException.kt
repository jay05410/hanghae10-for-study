package io.hhplus.ecommerce.pricing.exception

/**
 * Pricing 도메인 예외
 *
 * 가격 계산 과정에서 발생하는 비즈니스 예외를 정의
 */
sealed class PricingException(
    override val message: String,
    val errorCode: String
) : RuntimeException(message) {

    /**
     * 상품을 찾을 수 없는 경우
     */
    class ProductNotFound(productId: Long) : PricingException(
        message = "상품을 찾을 수 없습니다: productId=$productId",
        errorCode = "PRICING_PRODUCT_NOT_FOUND"
    )

    /**
     * 카테고리를 찾을 수 없는 경우
     */
    class CategoryNotFound(categoryId: Long) : PricingException(
        message = "카테고리를 찾을 수 없습니다: categoryId=$categoryId",
        errorCode = "PRICING_CATEGORY_NOT_FOUND"
    )

    /**
     * 쿠폰을 적용할 수 없는 경우
     */
    class CouponNotApplicable(reason: String) : PricingException(
        message = "쿠폰을 적용할 수 없습니다: $reason",
        errorCode = "PRICING_COUPON_NOT_APPLICABLE"
    )

    /**
     * 잘못된 가격 계산 요청
     */
    class InvalidPricingRequest(reason: String) : PricingException(
        message = "잘못된 가격 계산 요청: $reason",
        errorCode = "PRICING_INVALID_REQUEST"
    )

    /**
     * 사용자 쿠폰을 찾을 수 없는 경우
     */
    class UserCouponNotFound(userId: Long, userCouponId: Long) : PricingException(
        message = "사용자 쿠폰을 찾을 수 없습니다: userId=$userId, userCouponId=$userCouponId",
        errorCode = "PRICING_USER_COUPON_NOT_FOUND"
    )
}
