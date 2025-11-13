package io.hhplus.ecommerce.coupon.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "쿠폰 발급 요청")
data class IssueCouponRequest(
    @Schema(description = "발급할 쿠폰 ID", example = "1", required = true)
    val couponId: Long
)

@Schema(description = "쿠폰 사용 요청")
data class UseCouponRequest(
    @Schema(description = "사용할 사용자 쿠폰 ID", example = "10", required = true)
    val userCouponId: Long,

    @Schema(description = "주문 금액", example = "50000", required = true)
    val orderAmount: Long
)