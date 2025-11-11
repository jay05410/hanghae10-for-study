package io.hhplus.ecommerce.cart.dto

data class AddToCartRequest(
    val productId: Long,
    val boxTypeId: Long,
    val quantity: Int,
    val teaItems: List<TeaItemRequest> = emptyList()
)