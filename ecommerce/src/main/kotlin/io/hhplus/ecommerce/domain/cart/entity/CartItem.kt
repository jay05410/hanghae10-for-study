package io.hhplus.ecommerce.domain.cart.entity

import io.hhplus.ecommerce.domain.cart.vo.Quantity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "cart_items")
class CartItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    val cart: Cart,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val boxTypeId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedBy: Long
) {
    fun updateQuantity(newQuantity: Quantity, updatedBy: Long) {
        this.quantity = newQuantity.value
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy
    }

    companion object {
        fun create(
            cart: Cart,
            productId: Long,
            boxTypeId: Long,
            quantity: Quantity,
            createdBy: Long
        ): CartItem {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(boxTypeId > 0) { "박스 타입 ID는 유효해야 합니다" }

            return CartItem(
                cart = cart,
                productId = productId,
                boxTypeId = boxTypeId,
                quantity = quantity.value,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}