package io.hhplus.ecommerce.order.dto

/** Controller 요청 DTO */
data class CreateOrderRequest(
    val userId: Long,
    val items: List<CreateOrderItemRequest>,
    val usedCouponId: Long? = null
)

data class CreateOrderItemRequest(
    val packageTypeId: Long,
    val packageTypeName: String,
    val packageTypeDays: Int,
    val dailyServing: Int = 1,
    val totalQuantity: Double,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val quantity: Int,
    val containerPrice: Int,
    val teaPrice: Int,
    val giftWrapPrice: Int = 0,
    val teaItems: List<io.hhplus.ecommerce.cart.dto.TeaItemRequest> = emptyList(),
    // 하위 호환성을 위한 필드들 (deprecated)
    @Deprecated("Use packageTypeId instead")
    val productId: Long = packageTypeId,
    @Deprecated("Use packageTypeId instead")
    val boxTypeId: Long = packageTypeId
)

data class OrderConfirmRequest(
    val confirmedBy: Long
)

data class OrderCancelRequest(
    val cancelledBy: Long,
    val reason: String? = null
)