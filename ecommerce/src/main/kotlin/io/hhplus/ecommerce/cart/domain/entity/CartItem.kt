package io.hhplus.ecommerce.cart.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*
import java.time.LocalDateTime

// @Entity
// @Table(name = "cart_items")
class CartItem(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "cart_id", nullable = false)
    val cart: Cart,

    // @Column(nullable = false)
    val productId: Long,

    // @Column(nullable = false)
    val boxTypeId: Long,

    // @Column(nullable = false)
    var quantity: Int
) : ActiveJpaEntity() {
    fun updateQuantity(newQuantity: Int, updatedBy: Long) {
        require(newQuantity > 0) { "수량은 0보다 커야 합니다" }
        this.quantity = newQuantity
    }

    companion object {
        fun create(
            cart: Cart,
            productId: Long,
            boxTypeId: Long,
            quantity: Int,
            createdBy: Long
        ): CartItem {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(boxTypeId > 0) { "박스 타입 ID는 유효해야 합니다" }
            require(quantity > 0) { "수량은 0보다 커야 합니다" }

            return CartItem(
                cart = cart,
                productId = productId,
                boxTypeId = boxTypeId,
                quantity = quantity
            )
        }
    }
}