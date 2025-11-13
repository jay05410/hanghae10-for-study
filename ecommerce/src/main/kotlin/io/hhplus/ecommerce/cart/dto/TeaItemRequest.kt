package io.hhplus.ecommerce.cart.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "차 아이템 요청")
data class TeaItemRequest(
    @Schema(description = "차 상품 ID", example = "5", required = true)
    val productId: Long,

    @Schema(description = "선택 순서", example = "1", required = true)
    val selectionOrder: Int,

    @Schema(description = "비율 (퍼센트)", example = "50", required = true)
    val ratioPercent: Int
) {
    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use ratioPercent instead", ReplaceWith("ratioPercent"))
    @get:Schema(hidden = true)
    val quantity: Int
        get() = ratioPercent
}