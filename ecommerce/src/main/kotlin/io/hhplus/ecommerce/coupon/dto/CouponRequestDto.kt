package io.hhplus.ecommerce.coupon.dto

data class IssueCouponRequest(
    val couponId: Long
)

data class UseCouponRequest(
    val userCouponId: Long,
    val orderAmount: Long
)