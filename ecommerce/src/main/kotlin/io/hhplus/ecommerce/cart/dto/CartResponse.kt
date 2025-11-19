package io.hhplus.ecommerce.cart.dto

import io.swagger.v3.oas.annotations.media.Schema

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.entity.CartItem

/**
 * 장바구니 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - Cart 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
@Schema(description = "장바구니 정보")
data class CartResponse(
    @Schema(description = "장바구니 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "장바구니 아이템 목록")
    val items: List<CartItemResponse>,

    @Schema(description = "총 아이템 개수", example = "3")
    val totalItemCount: Int,

    @Schema(description = "총 수량", example = "5")
    val totalQuantity: Int,

    @Schema(description = "총 가격", example = "50000")
    val totalPrice: Long
) {
}

/**
 * 장바구니 아이템 정보 응답 DTO
 */
@Schema(description = "장바구니 아이템 정보")
data class CartItemResponse(
    @Schema(description = "아이템 ID", example = "1")
    val id: Long,

    @Schema(description = "상품 ID", example = "10")
    val productId: Long,

    @Schema(description = "수량", example = "2")
    val quantity: Int,


    @Schema(description = "선물 포장 여부", example = "true")
    val giftWrap: Boolean,

    @Schema(description = "선물 메시지 (선택)", example = "생일 축하합니다!")
    val giftMessage: String?
)

fun Cart.toResponse(): CartResponse = CartResponse(
    id = this.id,
    userId = this.userId,
    items = this.items.map { it.toResponse() },
    totalItemCount = this.getTotalItemCount(),
    totalQuantity = this.getTotalQuantity(),
    totalPrice = this.getTotalPrice()
)

fun CartItem.toResponse(): CartItemResponse = CartItemResponse(
    id = this.id,
    productId = this.productId,
    quantity = this.quantity,
    giftWrap = this.giftWrap,
    giftMessage = this.giftMessage
)