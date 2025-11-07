package io.hhplus.ecommerce.cart.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*

// @Entity
// @Table(name = "cart_item_tea")
class CartItemTea(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val cartItemId: Long,

    // @Column(nullable = false)
    val productId: Long,

    // @Column(nullable = false)
    val quantity: Int
) : ActiveJpaEntity() {

    fun validateQuantity() {
        require(quantity > 0) { "차 수량은 0보다 커야 합니다: $quantity" }
    }

    companion object {
        fun create(
            cartItemId: Long,
            productId: Long,
            quantity: Int
        ): CartItemTea {
            require(quantity > 0) { "차 수량은 0보다 커야 합니다" }

            return CartItemTea(
                cartItemId = cartItemId,
                productId = productId,
                quantity = quantity
            ).also { it.validateQuantity() }
        }
    }
}