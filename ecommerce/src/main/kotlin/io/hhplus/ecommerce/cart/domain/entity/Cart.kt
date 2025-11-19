package io.hhplus.ecommerce.cart.domain.entity

import io.hhplus.ecommerce.cart.exception.CartException
import java.time.LocalDateTime

/**
 * 장바구니 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 사용자별 장바구니 정보 관리
 * - 장바구니 아이템 추가/수정/삭제
 * - 장바구니 비즈니스 규칙 검증
 *
 * 비즈니스 규칙:
 * - 사용자당 장바구니 1개 (1:1 관계)
 * - 최대 50개 아이템 제한
 * - 동일 패키지 타입 중복 불가
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/CartJpaEntity에서 처리됩니다.
 */
data class Cart(
    val id: Long = 0,
    val userId: Long,
    private val _items: MutableList<CartItem> = mutableListOf()
) {
    val items: List<CartItem> get() = _items.toList()

    /**
     * 장바구니에 새 아이템 추가
     *
     * @return 추가된 CartItem
     * @throws IllegalArgumentException 최대 아이템 수 초과 또는 중복 상품
     */
    fun addItem(
        productId: Long,
        quantity: Int,
        giftWrap: Boolean = false,
        giftMessage: String? = null
    ): CartItem {
        require(_items.size < MAX_CART_ITEMS) { "장바구니 최대 아이템 수($MAX_CART_ITEMS)를 초과할 수 없습니다" }
        require(_items.none { it.productId == productId }) { "이미 동일한 상품이 장바구니에 있습니다" }

        val cartItem = CartItem.create(
            cartId = this.id,
            productId = productId,
            quantity = quantity,
            giftWrap = giftWrap,
            giftMessage = giftMessage
        )

        _items.add(cartItem)

        return cartItem
    }

    /**
     * 장바구니 아이템 수량 업데이트
     *
     * @throws CartException.CartItemNotFound 아이템을 찾을 수 없는 경우
     */
    fun updateItemQuantity(cartItemId: Long, newQuantity: Int) {
        val item = findItem(cartItemId)
        item.updateQuantity(newQuantity)
    }

    /**
     * 장바구니 아이템 전체 정보 업데이트 (수량, 선물 옵션 포함)
     * 기존 아이템을 제거하고 새 아이템으로 교체
     *
     * @throws CartException.CartItemNotFound 아이템을 찾을 수 없는 경우
     */
    fun updateItem(cartItemId: Long, quantity: Int, giftWrap: Boolean, giftMessage: String?) {
        val existingItem = findItem(cartItemId)
        val productId = existingItem.productId

        // 기존 아이템 제거
        _items.remove(existingItem)

        // 새 아이템 추가
        val newCartItem = CartItem.create(
            cartId = this.id,
            productId = productId,
            quantity = quantity,
            giftWrap = giftWrap,
            giftMessage = giftMessage
        )

        _items.add(newCartItem)
    }

    /**
     * 장바구니에서 아이템 제거
     *
     * @throws CartException.CartItemNotFound 아이템을 찾을 수 없는 경우
     */
    fun removeItem(cartItemId: Long) {
        val item = findItem(cartItemId)
        _items.remove(item)
    }

    /**
     * 장바구니 전체 비우기
     */
    fun clear() {
        _items.clear()
    }

    fun isEmpty(): Boolean = items.isEmpty()

    fun getTotalItemCount(): Int = items.size

    fun getTotalQuantity(): Int = items.sumOf { it.quantity }

    fun getTotalPrice(): Long {
        // TODO: ProductService를 통해 실제 상품 가격을 가져와서 계산해야 함
        // 현재는 임시로 0 반환
        return 0L
    }

    private fun findItem(cartItemId: Long): CartItem {
        return items.find { it.id == cartItemId }
            ?: throw CartException.CartItemNotFound(cartItemId)
    }

    companion object {
        private const val MAX_CART_ITEMS = 50

        /**
         * 장바구니 생성 팩토리 메서드
         *
         * @param userId 사용자 ID
         * @param createdBy 생성자 ID
         * @return 생성된 Cart 도메인 모델
         */
        fun create(userId: Long): Cart {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }

            return Cart(
                userId = userId
            )
        }
    }
}