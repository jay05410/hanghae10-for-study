package io.hhplus.ecommerce.order.dto

import io.hhplus.ecommerce.cart.dto.TeaItemRequest

/** Service 내부 DTO */
data class OrderItemData(
    val productId: Long,
    val boxTypeId: Long,
    val quantity: Int,
    val unitPrice: Long,
    val teaItems: List<TeaItemRequest> = emptyList()
)