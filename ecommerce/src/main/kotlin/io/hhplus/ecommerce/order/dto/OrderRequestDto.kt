package io.hhplus.ecommerce.order.dto

/** Controller 요청 DTO */
data class CreateOrderRequest(
    val userId: Long,
    val items: List<CreateOrderItemRequest>,
    val usedCouponId: Long? = null
)

data class CreateOrderItemRequest(
    val productId: Long,
    val boxTypeId: Long,
    val quantity: Int,
    val teaItems: List<io.hhplus.ecommerce.cart.dto.TeaItemRequest> = emptyList()
)

data class OrderConfirmRequest(
    val confirmedBy: Long
)

data class OrderCancelRequest(
    val cancelledBy: Long,
    val reason: String? = null
)