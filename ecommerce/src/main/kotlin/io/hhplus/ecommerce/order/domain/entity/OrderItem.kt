package io.hhplus.ecommerce.order.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
//import jakarta.persistence.*

//@Entity
//@Table(name = "order_items")
class OrderItem(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

//    @Column(nullable = false)
    val productId: Long,

//    @Column(nullable = false)
    val boxTypeId: Long,

//    @Column(nullable = false)
    val quantity: Int,

//    @Column(nullable = false)
    val unitPrice: Long,

//    @Column(nullable = false)
    val totalPrice: Long
) : ActiveJpaEntity() {
    companion object {
        fun create(
            order: Order,
            productId: Long,
            boxTypeId: Long,
            quantity: Int,
            unitPrice: Long
        ): OrderItem {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(boxTypeId > 0) { "박스 타입 ID는 유효해야 합니다" }
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(unitPrice > 0) { "단가는 0보다 커야 합니다" }

            val totalPrice = unitPrice * quantity

            return OrderItem(
                order = order,
                productId = productId,
                boxTypeId = boxTypeId,
                quantity = quantity,
                unitPrice = unitPrice,
                totalPrice = totalPrice
            )
        }
    }
}