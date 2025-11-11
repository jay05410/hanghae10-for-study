package io.hhplus.ecommerce.order.dto

import io.hhplus.ecommerce.cart.dto.TeaItemRequest

/** Service 내부 DTO */
data class OrderItemData(
    val packageTypeId: Long,
    val packageTypeName: String,
    val packageTypeDays: Int,
    val dailyServing: Int,
    val totalQuantity: Double,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val quantity: Int,
    val containerPrice: Int,
    val teaPrice: Int,
    val giftWrapPrice: Int = 0,
    val teaItems: List<TeaItemRequest> = emptyList()
) {
    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use packageTypeId instead", ReplaceWith("packageTypeId"))
    val productId: Long
        get() = packageTypeId

    @Deprecated("Use packageTypeId instead", ReplaceWith("packageTypeId"))
    val boxTypeId: Long
        get() = packageTypeId

    @Deprecated("Use teaPrice instead", ReplaceWith("teaPrice.toLong()"))
    val unitPrice: Long
        get() = teaPrice.toLong()
}