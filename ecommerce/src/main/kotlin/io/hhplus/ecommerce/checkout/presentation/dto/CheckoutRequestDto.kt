package io.hhplus.ecommerce.checkout.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 주문하기 요청 (체크아웃 시작)
 *
 * 두 가지 방식 지원:
 * 1. 장바구니에서 주문: cartItemIds 사용
 * 2. 바로 주문: directOrderItems 사용
 *
 * 둘 중 하나는 반드시 있어야 함
 */
@Schema(description = "주문하기 요청")
data class InitiateCheckoutRequest(
    @Schema(description = "사용자 ID", example = "100", required = true)
    val userId: Long,

    @Schema(description = "장바구니 아이템 ID 목록 (장바구니에서 주문 시)", example = "[1, 2, 3]")
    val cartItemIds: List<Long>? = null,

    @Schema(description = "바로 주문 상품 목록 (상품에서 바로 주문 시)")
    val directOrderItems: List<DirectOrderItem>? = null
) {
    fun validate() {
        require(cartItemIds != null || directOrderItems != null) {
            "cartItemIds 또는 directOrderItems 중 하나는 반드시 있어야 합니다"
        }
        require(!(cartItemIds != null && directOrderItems != null)) {
            "cartItemIds와 directOrderItems를 동시에 사용할 수 없습니다"
        }
        cartItemIds?.let {
            require(it.isNotEmpty()) { "장바구니 아이템을 선택해주세요" }
        }
        directOrderItems?.let {
            require(it.isNotEmpty()) { "주문할 상품을 선택해주세요" }
        }
    }

    fun isFromCart(): Boolean = cartItemIds != null
}

/**
 * 바로 주문 상품
 */
@Schema(description = "바로 주문 상품")
data class DirectOrderItem(
    @Schema(description = "상품 ID", example = "10", required = true)
    val productId: Long,

    @Schema(description = "수량", example = "2", required = true)
    val quantity: Int,

    @Schema(description = "선물 포장 여부", example = "false", defaultValue = "false")
    val giftWrap: Boolean = false,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
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

/**
 * 선착순 체크아웃 요청 (Kafka 큐 방식)
 *
 * 재고가 제한된 인기 상품 주문 시 사용
 * - 즉시 응답: 대기열 순번 반환
 * - 비동기 처리: Kafka Consumer가 순차 처리
 * - 결과 알림: SSE로 체크아웃 완료/실패 푸시
 */
@Schema(description = "선착순 체크아웃 요청")
data class QueuedCheckoutRequest(
    @Schema(description = "사용자 ID", example = "100", required = true)
    val userId: Long,

    @Schema(description = "상품 ID (선착순 상품)", example = "1", required = true)
    val productId: Long,

    @Schema(description = "수량", example = "1", required = true)
    val quantity: Int = 1
)
