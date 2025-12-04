package io.hhplus.ecommerce.order.presentation.dto

import io.hhplus.ecommerce.delivery.presentation.dto.DeliveryAddressRequest
import io.swagger.v3.oas.annotations.media.Schema

/** Controller 요청 DTO */
@Schema(description = "주문 생성 요청")
data class CreateOrderRequest(
    @Schema(description = "사용자 ID", example = "100", required = true)
    val userId: Long,

    @Schema(description = "주문 아이템 목록", required = true)
    val items: List<CreateOrderItemRequest>,

    @Schema(description = "배송지 정보", required = true)
    val deliveryAddress: DeliveryAddressRequest,

    @Schema(description = "사용할 쿠폰 ID (선택)", example = "10")
    val usedCouponId: Long? = null
)

@Schema(description = "주문 아이템 생성 요청")
data class CreateOrderItemRequest(
    @Schema(description = "상품 ID", example = "10", required = true)
    val productId: Long,

    @Schema(description = "수량", example = "2", required = true)
    val quantity: Int,

    @Schema(description = "선물 포장 여부", example = "true", defaultValue = "false")
    val giftWrap: Boolean = false,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String? = null
)

@Schema(description = "주문 확정 요청")
data class OrderConfirmRequest(
    @Schema(description = "확정 처리자 ID", example = "1", required = true)
    val confirmedBy: Long
)

@Schema(description = "주문 취소 요청")
data class OrderCancelRequest(
    @Schema(description = "취소 처리자 ID", example = "1", required = true)
    val cancelledBy: Long,

    @Schema(description = "취소 사유 (선택)", example = "고객 변심")
    val reason: String? = null
)
