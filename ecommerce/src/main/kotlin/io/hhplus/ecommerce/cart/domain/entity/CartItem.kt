package io.hhplus.ecommerce.cart.domain.entity

import java.time.LocalDateTime

/**
 * 장바구니 아이템 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 장바구니에 담긴 개별 상품 정보 관리
 * - 상품, 수량, 선물 옵션 등 관리
 *
 * 비즈니스 규칙:
 * - 수량은 0보다 커야 함
 * - 선물 포장 시 선물 메시지 선택 가능
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/CartItemJpaEntity에서 처리됩니다.
 */
data class CartItem(
    val id: Long = 0,
    val cartId: Long,
    val productId: Long,
    var quantity: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null
) {
    fun validateQuantity() {
        require(quantity > 0) { "수량은 0보다 커야 합니다: $quantity" }
    }

    fun updateQuantity(newQuantity: Int) {
        require(newQuantity > 0) { "수량은 0보다 커야 합니다: $newQuantity" }
        this.quantity = newQuantity
    }

    companion object {
        /**
         * 장바구니 아이템 생성 팩토리 메서드
         *
         * @return 생성된 CartItem 도메인 모델
         */
        fun create(
            cartId: Long,
            productId: Long,
            quantity: Int,
            giftWrap: Boolean = false,
            giftMessage: String? = null
        ): CartItem {
            require(cartId > 0) { "장바구니 ID는 유효해야 합니다" }
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(quantity > 0) { "수량은 0보다 커야 합니다" }

            return CartItem(
                cartId = cartId,
                productId = productId,
                quantity = quantity,
                giftWrap = giftWrap,
                giftMessage = giftMessage
            ).also { it.validateQuantity() }
        }
    }
}