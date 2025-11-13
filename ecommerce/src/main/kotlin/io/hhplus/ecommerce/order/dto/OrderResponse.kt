package io.hhplus.ecommerce.order.dto

import io.swagger.v3.oas.annotations.media.Schema

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
@Schema(description = "주문 정보")
data class OrderResponse(
    @Schema(description = "주문 ID", example = "1")
    val id: Long,

    @Schema(description = "주문 번호", example = "ORD-20250113-001")
    val orderNumber: String,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "총 주문 금액 (할인 전)", example = "50000")
    val totalAmount: Long,

    @Schema(description = "할인 금액", example = "5000")
    val discountAmount: Long,

    @Schema(description = "최종 결제 금액 (할인 후)", example = "45000")
    val finalAmount: Long,

    @Schema(description = "사용한 쿠폰 ID (선택)", example = "10")
    val usedCouponId: Long?,

    @Schema(description = "주문 상태", example = "PENDING", allowableValues = ["PENDING", "CONFIRMED", "CANCELLED"])
    val status: OrderStatus,

    @Schema(description = "주문 아이템 목록")
    val orderItems: List<OrderItemResponse>,

    @Schema(description = "주문 생성 일시", example = "2025-01-13T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "주문 수정 일시", example = "2025-01-13T14:30:00")
    val updatedAt: LocalDateTime
) {
}

/**
 * 주문 아이템 정보 응답 DTO
 */
@Schema(description = "주문 아이템 정보")
data class OrderItemResponse(
    @Schema(description = "주문 아이템 ID", example = "1")
    val id: Long,

    @Schema(description = "패키지 타입 ID", example = "10")
    val packageTypeId: Long,

    @Schema(description = "패키지 타입 이름", example = "30일 정기배송")
    val packageTypeName: String,

    @Schema(description = "패키지 일수", example = "30")
    val packageTypeDays: Int,

    @Schema(description = "일일 제공량", example = "2")
    val dailyServing: Int,

    @Schema(description = "총 수량", example = "2.0")
    val totalQuantity: Double,

    @Schema(description = "선물 포장 여부", example = "true")
    val giftWrap: Boolean,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String?,

    @Schema(description = "주문 수량", example = "1")
    val quantity: Int,

    @Schema(description = "용기 가격", example = "5000")
    val containerPrice: Int,

    @Schema(description = "차 가격", example = "30000")
    val teaPrice: Int,

    @Schema(description = "선물 포장 가격", example = "3000")
    val giftWrapPrice: Int,

    @Schema(description = "아이템 총 가격", example = "38000")
    val totalPrice: Int,

    @Schema(description = "생성 일시", example = "2025-01-13T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 일시", example = "2025-01-13T14:30:00")
    val updatedAt: LocalDateTime
)

fun Order.toResponse(orderItems: List<OrderItem> = emptyList()): OrderResponse = OrderResponse(
    id = this.id,
    orderNumber = this.orderNumber,
    userId = this.userId,
    totalAmount = this.totalAmount,
    discountAmount = this.discountAmount,
    finalAmount = this.finalAmount,
    usedCouponId = this.usedCouponId,
    status = this.status,
    orderItems = orderItems.map { it.toResponse() },
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