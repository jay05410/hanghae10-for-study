package io.hhplus.ecommerce.order.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.common.exception.order.OrderException
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
//import jakarta.persistence.*

//@Entity
//@Table(name = "orders")
class Order(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @Column(nullable = false, unique = true, length = 50)
    val orderNumber: String,

//    @Column(nullable = false)
    val userId: Long,

//    @Column(nullable = false)
    val totalAmount: Long,

//    @Column(nullable = false)
    val discountAmount: Long = 0,

//    @Column(nullable = false)
    val finalAmount: Long,

//    @Column(nullable = false)
    val usedCouponId: Long? = null,

//    @Column(nullable = false, length = 20)
//    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING,

//    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    private val _items: MutableList<OrderItem> = mutableListOf()
) : ActiveJpaEntity() {
    val items: List<OrderItem> get() = _items.toList()


    fun addItem(productId: Long, boxTypeId: Long, quantity: Int, unitPrice: Long): OrderItem {
        val orderItem = OrderItem.create(
            order = this,
            productId = productId,
            boxTypeId = boxTypeId,
            quantity = quantity,
            unitPrice = unitPrice
        )

        _items.add(orderItem)
        return orderItem
    }

    fun confirm(confirmedBy: Long) {
        validateStatusTransition(OrderStatus.CONFIRMED)
        this.status = OrderStatus.CONFIRMED
        updateAuditInfo(confirmedBy)
    }

    fun cancel(cancelledBy: Long) {
        // 취소 가능 상태 검증: PENDING, CONFIRMED 상태만 취소 가능
        if (!canBeCancelled()) {
            throw OrderException.OrderCancellationNotAllowed(orderNumber, status)
        }

        this.status = OrderStatus.CANCELLED
        updateAuditInfo(cancelledBy)
    }

    fun complete(completedBy: Long) {
        validateStatusTransition(OrderStatus.COMPLETED)
        this.status = OrderStatus.COMPLETED
        updateAuditInfo(completedBy)
    }

    fun fail(failedBy: Long) {
        this.status = OrderStatus.FAILED
        updateAuditInfo(failedBy)
    }

    fun canBeCancelled(): Boolean = status.canBeCancelled()

    fun isPaid(): Boolean = status.isPaid()

    private fun validateStatusTransition(newStatus: OrderStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw OrderException.InvalidOrderStatus(orderNumber, status, newStatus)
        }
    }

    companion object {
        fun create(
            orderNumber: String,
            userId: Long,
            totalAmount: Long,
            discountAmount: Long = 0,
            usedCouponId: Long? = null
        ): Order {
            require(orderNumber.isNotBlank()) { "주문번호는 필수입니다" }
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(totalAmount > 0) { "총 금액은 0보다 커야 합니다" }
            require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다" }

            val finalAmount = totalAmount - discountAmount
            require(finalAmount >= 0) { "최종 금액은 0 이상이어야 합니다" }

            val order = Order(
                orderNumber = orderNumber,
                userId = userId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                usedCouponId = usedCouponId
            )
            return order
        }
    }
}