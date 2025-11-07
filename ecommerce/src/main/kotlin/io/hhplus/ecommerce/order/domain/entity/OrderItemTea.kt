package io.hhplus.ecommerce.order.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
//import jakarta.persistence.*

//@Entity
//@Table(name = "order_item_tea")
class OrderItemTea(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @Column(nullable = false)
    val orderItemId: Long,

//    @Column(nullable = false)
    val productId: Long,

//    @Column(nullable = false)
    val quantity: Int
) : ActiveJpaEntity() {

    fun validateQuantity() {
        require(quantity > 0) { "차 수량은 0보다 커야 합니다: $quantity" }
    }

    companion object {
        fun create(
            orderItemId: Long,
            productId: Long,
            quantity: Int
        ): OrderItemTea {
            require(quantity > 0) { "차 수량은 0보다 커야 합니다" }

            return OrderItemTea(
                orderItemId = orderItemId,
                productId = productId,
                quantity = quantity
            ).also { it.validateQuantity() }
        }
    }
}