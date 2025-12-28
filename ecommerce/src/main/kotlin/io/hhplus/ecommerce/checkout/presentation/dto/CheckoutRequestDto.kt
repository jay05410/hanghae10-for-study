package io.hhplus.ecommerce.checkout.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 체크아웃 요청
 *
 * 체크아웃 = 결제 버튼 클릭 시점, 재고 락 확보
 *
 * 지원 방식:
 * 1. 장바구니 주문: cartItemIds 사용
 * 2. 바로 주문: items 사용
 *
 * 처리 흐름:
 * - Kafka 큐 발행 → 순차 처리 → SSE 결과 푸시
 * - Kafka 실패 시 → 동기 폴백
 */
@Schema(description = "체크아웃 요청")
data class CheckoutRequest(
    @Schema(description = "사용자 ID", example = "100", required = true)
    val userId: Long,

    @Schema(description = "장바구니 아이템 ID 목록", example = "[1, 2, 3]")
    val cartItemIds: List<Long>? = null,

    @Schema(description = "바로 주문 상품 목록")
    val items: List<CheckoutItem>? = null
) {
    fun validate() {
        require(cartItemIds != null || items != null) {
            "cartItemIds 또는 items 중 하나는 반드시 있어야 합니다"
        }
        require(!(cartItemIds != null && items != null)) {
            "cartItemIds와 items를 동시에 사용할 수 없습니다"
        }
    }

    fun isFromCart(): Boolean = cartItemIds != null
}

/**
 * 체크아웃 상품 아이템
 */
@Schema(description = "체크아웃 상품")
data class CheckoutItem(
    @Schema(description = "상품 ID", example = "1", required = true)
    val productId: Long,

    @Schema(description = "수량", example = "1", required = true)
    val quantity: Int = 1,

    @Schema(description = "선물 포장 여부", defaultValue = "false")
    val giftWrap: Boolean = false,

    @Schema(description = "선물 메시지")
    val giftMessage: String? = null
)

/**
 * 체크아웃 취소 요청
 */
@Schema(description = "체크아웃 취소 요청")
data class CancelCheckoutRequest(
    @Schema(description = "사용자 ID", example = "100", required = true)
    val userId: Long,

    @Schema(description = "취소 사유 (선택)", example = "결제 포기")
    val reason: String? = null
)
