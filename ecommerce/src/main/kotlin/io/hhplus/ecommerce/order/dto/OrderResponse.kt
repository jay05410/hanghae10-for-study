package io.hhplus.ecommerce.order.dto

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import java.time.LocalDateTime

/**
 * 주문 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - Order 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
    val usedCouponId: Long?,
    val status: OrderStatus,
    val orderItems: List<OrderItemResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
}

/**
 * 주문 아이템 정보 응답 DTO
 */
data class OrderItemResponse(
    val id: Long,
    val packageTypeId: Long,
    val packageTypeName: String,
    val packageTypeDays: Int,
    val dailyServing: Int,
    val totalQuantity: Double,
    val giftWrap: Boolean,
    val giftMessage: String?,
    val quantity: Int,
    val containerPrice: Int,
    val teaPrice: Int,
    val giftWrapPrice: Int,
    val totalPrice: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun Order.toResponse(): OrderResponse = OrderResponse(
    id = this.id,
    orderNumber = this.orderNumber,
    userId = this.userId,
    totalAmount = this.totalAmount,
    discountAmount = this.discountAmount,
    finalAmount = this.finalAmount,
    usedCouponId = this.usedCouponId,
    status = this.status,
    orderItems = this.items.map { it.toResponse() },
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun OrderItem.toResponse(): OrderItemResponse = OrderItemResponse(
    id = this.id,
    packageTypeId = this.packageTypeId,
    packageTypeName = this.packageTypeName,
    packageTypeDays = this.packageTypeDays,
    dailyServing = this.dailyServing,
    totalQuantity = this.totalQuantity,
    giftWrap = this.giftWrap,
    giftMessage = this.giftMessage,
    quantity = this.quantity,
    containerPrice = this.containerPrice,
    teaPrice = this.teaPrice,
    giftWrapPrice = this.giftWrapPrice,
    totalPrice = this.totalPrice,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)