package io.hhplus.ecommerce.order.dto

import io.swagger.v3.oas.annotations.media.Schema

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.constant.OrderStatus

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
    val orderItems: List<OrderItemResponse>
) {
}

/**
 * 주문 아이템 정보 응답 DTO
 */
@Schema(description = "주문 아이템 정보")
data class OrderItemResponse(
    @Schema(description = "주문 아이템 ID", example = "1")
    val id: Long,

    @Schema(description = "상품 ID", example = "10")
    val productId: Long,

    @Schema(description = "상품명", example = "테스트 상품")
    val productName: String,

    @Schema(description = "카테고리명", example = "전자기기")
    val categoryName: String,

    @Schema(description = "수량", example = "2")
    val quantity: Int,

    @Schema(description = "단가", example = "10000")
    val unitPrice: Int,

    @Schema(description = "선물 포장 여부", example = "true")
    val giftWrap: Boolean,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String?,


    @Schema(description = "선물 포장 가격", example = "3000")
    val giftWrapPrice: Int,

    @Schema(description = "아이템 총 가격", example = "38000")
    val totalPrice: Int
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
    orderItems = orderItems.map { it.toResponse() }
)

fun OrderItem.toResponse(): OrderItemResponse = OrderItemResponse(
    id = this.id,
    productId = this.productId,
    productName = this.productName,
    categoryName = this.categoryName,
    quantity = this.quantity,
    unitPrice = this.unitPrice,
    giftWrap = this.giftWrap,
    giftMessage = this.giftMessage,
    giftWrapPrice = this.giftWrapPrice,
    totalPrice = this.totalPrice
)