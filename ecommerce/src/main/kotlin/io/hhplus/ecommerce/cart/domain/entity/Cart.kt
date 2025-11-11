package io.hhplus.ecommerce.cart.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.common.exception.cart.CartException
// import jakarta.persistence.*
import java.time.LocalDateTime

// @Entity
// @Table(name = "carts")
class Cart(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true)
    val userId: Long,

    // @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    private val _items: MutableList<CartItem> = mutableListOf()
) : ActiveJpaEntity() {
    val items: List<CartItem> get() = _items.toList()

    fun addItem(productId: Long, boxTypeId: Long, quantity: Int, addedBy: Long): CartItem {
        require(_items.size < MAX_CART_ITEMS) { "장바구니 최대 아이템 수($MAX_CART_ITEMS)를 초과할 수 없습니다" }
        require(_items.none { it.boxTypeId == boxTypeId }) { "이미 동일한 박스 타입이 장바구니에 있습니다" }

        val cartItem = CartItem.create(
            cart = this,
            productId = productId,
            boxTypeId = boxTypeId,
            quantity = quantity,
            createdBy = addedBy
        )

        _items.add(cartItem)

        return cartItem
    }

    fun updateItemQuantity(cartItemId: Long, newQuantity: Int, updatedBy: Long): CartItem {
        val item = findItem(cartItemId)
        item.updateQuantity(newQuantity, updatedBy)

        return item
    }

    fun removeItem(cartItemId: Long, removedBy: Long) {
        val item = findItem(cartItemId)
        _items.remove(item)
    }

    fun clear(clearedBy: Long) {
        _items.clear()
    }

    fun isEmpty(): Boolean = _items.isEmpty()

    fun getTotalItemCount(): Int = _items.size

    fun getTotalQuantity(): Int = _items.sumOf { it.quantity }

    fun getTotalPrice(): Long {
        // TODO: ProductService를 통해 실제 상품 가격을 가져와서 계산해야 함
        // 현재는 임시로 0 반환
        return 0L
    }

    private fun findItem(cartItemId: Long): CartItem {
        return _items.find { it.id == cartItemId }
            ?: throw CartException.CartItemNotFound(cartItemId)
    }

    companion object {
        private const val MAX_CART_ITEMS = 50

        fun create(userId: Long, createdBy: Long): Cart {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }

            return Cart(userId = userId)
        }
    }
}