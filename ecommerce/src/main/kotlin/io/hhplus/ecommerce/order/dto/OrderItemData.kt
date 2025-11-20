package io.hhplus.ecommerce.order.dto

/** Service 내부 DTO */
data class OrderItemData(
    val productId: Long,
    val productName: String,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val giftWrapPrice: Int = 0,
    val totalPrice: Int,
    val requiresReservation: Boolean = false  // 선착순/한정판 여부
)