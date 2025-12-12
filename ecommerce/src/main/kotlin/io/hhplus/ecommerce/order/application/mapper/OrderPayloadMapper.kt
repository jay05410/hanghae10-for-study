package io.hhplus.ecommerce.order.application.mapper

import io.hhplus.ecommerce.common.client.OrderInfoPayload
import io.hhplus.ecommerce.common.client.OrderItemPayload
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Order 도메인 → DataPlatform Payload 변환 확장 함수
 */
fun Order.toOrderInfoPayload(
    items: List<OrderItem>,
    paymentId: Long? = null
): OrderInfoPayload {
    return OrderInfoPayload(
        orderId = this.id,
        orderNumber = this.orderNumber,
        userId = this.userId,
        items = items.map { it.toOrderItemPayload() },
        totalAmount = this.totalAmount,
        discountAmount = this.discountAmount,
        finalAmount = this.finalAmount,
        status = this.status.name,
        paymentId = paymentId,
        createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

fun OrderItem.toOrderItemPayload(): OrderItemPayload {
    return OrderItemPayload(
        productId = this.productId,
        productName = this.productName,
        quantity = this.quantity,
        unitPrice = this.unitPrice.toLong(),
        totalPrice = this.totalPrice.toLong()
    )
}
