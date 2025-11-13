package io.hhplus.ecommerce.order.dto

import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
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
    @Schema(description = "패키지 타입 ID", example = "10", required = true)
    val packageTypeId: Long,

    @Schema(description = "패키지 타입 이름", example = "30일 정기배송", required = true)
    val packageTypeName: String,

    @Schema(description = "패키지 일수", example = "30", required = true)
    val packageTypeDays: Int,

    @Schema(description = "일일 제공량", example = "2", defaultValue = "1")
    val dailyServing: Int = 1,

    @Schema(description = "총 수량", example = "2.0", required = true)
    val totalQuantity: Double,

    @Schema(description = "선물 포장 여부", example = "true", defaultValue = "false")
    val giftWrap: Boolean = false,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String? = null,

    @Schema(description = "주문 수량", example = "1", required = true)
    val quantity: Int,

    @Schema(description = "용기 가격", example = "5000", required = true)
    val containerPrice: Int,

    @Schema(description = "차 가격", example = "30000", required = true)
    val teaPrice: Int,

    @Schema(description = "선물 포장 가격", example = "3000", defaultValue = "0")
    val giftWrapPrice: Int = 0,

    @Schema(description = "차 아이템 목록")
    val teaItems: List<io.hhplus.ecommerce.cart.dto.TeaItemRequest> = emptyList(),

    // 하위 호환성을 위한 필드들 (deprecated)
    @Deprecated("Use packageTypeId instead")
    @Schema(hidden = true)
    val productId: Long = packageTypeId,

    @Deprecated("Use packageTypeId instead")
    @Schema(hidden = true)
    val boxTypeId: Long = packageTypeId
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