package io.hhplus.ecommerce.cart.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "장바구니 아이템 추가 요청")
data class AddToCartRequest(
    @Schema(description = "패키지 타입 ID", example = "10", required = true)
    val packageTypeId: Long,

    @Schema(description = "패키지 타입 이름", example = "30일 정기배송", required = true)
    val packageTypeName: String,

    @Schema(description = "패키지 일수", example = "30", required = true)
    val packageTypeDays: Int,

    @Schema(description = "일일 제공량", example = "2", defaultValue = "1")
    val dailyServing: Int = 1,

    @Schema(description = "총 수량", example = "2.0", required = true)
    val totalQuantity: Double,

    @Schema(description = "선물 포장 여부", example = "true", defaultValue = "false")
    val giftWrap: Boolean = false,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String? = null,

    @Schema(description = "차 아이템 목록")
    val teaItems: List<TeaItemRequest> = emptyList(),

    // 하위 호환성을 위한 필드들 (deprecated)
    @Deprecated("Use packageTypeId instead")
    @Schema(hidden = true)
    val productId: Long = packageTypeId,

    @Deprecated("Use packageTypeId instead")
    @Schema(hidden = true)
    val boxTypeId: Long = packageTypeId,

    @Deprecated("Use totalQuantity instead")
    @Schema(hidden = true)
    val quantity: Int = totalQuantity.toInt()
)