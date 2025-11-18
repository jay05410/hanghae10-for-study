package io.hhplus.ecommerce.cart.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "장바구니 아이템 추가 요청")
data class AddToCartRequest(
    @Schema(description = "상품 ID", example = "10", required = true)
    val productId: Long,

    @Schema(description = "수량", example = "2", required = true)
    val quantity: Int,

    @Schema(description = "선물 포장 여부", example = "true", defaultValue = "false")
    val giftWrap: Boolean = false,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String? = null
)