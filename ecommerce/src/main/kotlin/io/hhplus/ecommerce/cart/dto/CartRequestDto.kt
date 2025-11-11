package io.hhplus.ecommerce.cart.dto

data class AddToCartRequest(
    val packageTypeId: Long,
    val packageTypeName: String,
    val packageTypeDays: Int,
    val dailyServing: Int = 1,
    val totalQuantity: Double,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val teaItems: List<TeaItemRequest> = emptyList(),
    // 하위 호환성을 위한 필드들 (deprecated)
    @Deprecated("Use packageTypeId instead")
    val productId: Long = packageTypeId,
    @Deprecated("Use packageTypeId instead")
    val boxTypeId: Long = packageTypeId,
    @Deprecated("Use totalQuantity instead")
    val quantity: Int = totalQuantity.toInt()
)