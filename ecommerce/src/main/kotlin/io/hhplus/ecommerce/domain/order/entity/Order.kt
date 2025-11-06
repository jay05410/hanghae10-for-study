package io.hhplus.ecommerce.domain.order.entity

import io.hhplus.ecommerce.common.exception.order.OrderException
import io.hhplus.ecommerce.domain.order.vo.OrderAmount
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val orderNumber: String,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val totalAmount: Long,

    @Column(nullable = false)
    val discountAmount: Long = 0,

    @Column(nullable = false)
    val finalAmount: Long,

    @Column(nullable = false)
    val usedCouponId: Long? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedBy: Long,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    private val _items: MutableList<OrderItem> = mutableListOf()
) {
    val items: List<OrderItem> get() = _items.toList()

    fun getOrderAmount(): OrderAmount = OrderAmount(totalAmount, discountAmount, finalAmount)

    fun addItem(productId: Long, boxTypeId: Long, quantity: Int, unitPrice: Long): OrderItem {
        val orderItem = OrderItem.create(
            order = this,
            productId = productId,
            boxTypeId = boxTypeId,
            quantity = quantity,
            unitPrice = unitPrice,
            createdBy = this.createdBy
        )

        _items.add(orderItem)
        return orderItem
    }

    fun confirm(confirmedBy: Long) {
        validateStatusTransition(OrderStatus.CONFIRMED)
        this.status = OrderStatus.CONFIRMED
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = confirmedBy
    }

    fun cancel(cancelledBy: Long) {
        // 취소 가능 상태 검증: PENDING, CONFIRMED 상태만 취소 가능
        if (!canBeCancelled()) {
            throw OrderException.OrderCancellationNotAllowed(orderNumber, status)
        }

        this.status = OrderStatus.CANCELLED
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = cancelledBy
    }

    fun complete(completedBy: Long) {
        validateStatusTransition(OrderStatus.COMPLETED)
        this.status = OrderStatus.COMPLETED
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = completedBy
    }

    fun fail(failedBy: Long) {
        this.status = OrderStatus.FAILED
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = failedBy
    }

    fun canBeCancelled(): Boolean = status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)

    fun isPaid(): Boolean = status in listOf(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)

    private fun validateStatusTransition(newStatus: OrderStatus) {
        val validTransitions = when (status) {
            OrderStatus.PENDING -> listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.FAILED)
            OrderStatus.CONFIRMED -> listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
            OrderStatus.COMPLETED -> emptyList()
            OrderStatus.CANCELLED -> emptyList()
            OrderStatus.FAILED -> emptyList()
        }

        if (newStatus !in validTransitions) {
            throw OrderException.InvalidOrderStatus(orderNumber, status, newStatus)
        }
    }

    companion object {
        fun create(
            orderNumber: String,
            userId: Long,
            totalAmount: Long,
            discountAmount: Long = 0,
            usedCouponId: Long? = null,
            createdBy: Long
        ): Order {
            require(orderNumber.isNotBlank()) { "주문번호는 필수입니다" }
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(totalAmount > 0) { "총 금액은 0보다 커야 합니다" }
            require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다" }

            val finalAmount = totalAmount - discountAmount
            require(finalAmount >= 0) { "최종 금액은 0 이상이어야 합니다" }

            return Order(
                orderNumber = orderNumber,
                userId = userId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                usedCouponId = usedCouponId,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}

enum class OrderStatus {
    PENDING, CONFIRMED, COMPLETED, CANCELLED, FAILED
}