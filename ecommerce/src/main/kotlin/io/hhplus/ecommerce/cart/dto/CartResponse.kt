package io.hhplus.ecommerce.cart.dto

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.entity.CartItem
import java.time.LocalDateTime

/**
 * 장바구니 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - Cart 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
data class CartResponse(
    val id: Long,
    val userId: Long,
    val items: List<CartItemResponse>,
    val totalItemCount: Int,
    val totalQuantity: Int,
    val totalPrice: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
}

/**
 * 장바구니 아이템 정보 응답 DTO
 */
data class CartItemResponse(
    val id: Long,
    val productId: Long,
    val boxTypeId: Long,
    val quantity: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun Cart.toResponse(): CartResponse = CartResponse(
            id = this.id,
            userId = this.userId,
            items = this.items.map { it.toResponse() },
            totalItemCount = this.getTotalItemCount(),
            totalQuantity = this.getTotalQuantity(),
            totalPrice = this.getTotalPrice(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )

        fun CartItem.toResponse(): CartItemResponse = CartItemResponse(
            id = this.id,
            productId = this.productId,
            boxTypeId = this.boxTypeId,
            quantity = this.quantity,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}