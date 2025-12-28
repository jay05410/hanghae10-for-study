package io.hhplus.ecommerce.checkout.presentation.dto

import io.hhplus.ecommerce.checkout.domain.model.CheckoutSession
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 체크아웃 응답 (주문창 정보)
 */
@Schema(description = "주문창 응답")
data class CheckoutResponse(
    @Schema(description = "주문 ID", example = "1")
    val orderId: Long,

    @Schema(description = "주문 번호", example = "ORD-20241201-001")
    val orderNumber: String,

    @Schema(description = "결제 만료 시간")
    val expiresAt: LocalDateTime,

    @Schema(description = "남은 시간 (초)", example = "600")
    val remainingSeconds: Long,

    @Schema(description = "총 금액", example = "50000")
    val totalAmount: Long,

    @Schema(description = "할인 금액", example = "5000")
    val discountAmount: Long,

    @Schema(description = "최종 결제 금액", example = "45000")
    val finalAmount: Long,

    @Schema(description = "주문 상품 목록")
    val items: List<CheckoutItemResponse>
)

/**
 * 체크아웃 상품 응답
 */
@Schema(description = "주문 상품 정보")
data class CheckoutItemResponse(
    @Schema(description = "상품 ID", example = "10")
    val productId: Long,

    @Schema(description = "수량", example = "2")
    val quantity: Int,

    @Schema(description = "선물 포장 여부", example = "false")
    val giftWrap: Boolean,

    @Schema(description = "선물 메시지")
    val giftMessage: String?
)

/**
 * CheckoutSession -> CheckoutResponse 변환
 */
fun CheckoutSession.toResponse(): CheckoutResponse = CheckoutResponse(
    orderId = orderId,
    orderNumber = orderNumber,
    expiresAt = expiresAt,
    remainingSeconds = getRemainingSeconds(),
    totalAmount = totalAmount,
    discountAmount = discountAmount,
    finalAmount = finalAmount,
    items = items.map { it.toResponse() }
)

/**
 * CheckoutItem -> CheckoutItemResponse 변환
 */
fun CheckoutItem.toResponse(): CheckoutItemResponse = CheckoutItemResponse(
    productId = productId,
    quantity = quantity,
    giftWrap = giftWrap,
    giftMessage = giftMessage
)
