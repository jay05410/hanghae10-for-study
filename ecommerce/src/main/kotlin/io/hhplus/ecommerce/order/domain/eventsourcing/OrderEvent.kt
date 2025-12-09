package io.hhplus.ecommerce.order.domain.eventsourcing

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import java.time.Instant

/**
 * 주문 도메인 이벤트 정의 (Event Sourcing)
 *
 * 주문의 모든 상태 변경을 이벤트로 표현
 * - 불변(immutable) 객체로 저장
 * - 시간순으로 재생하여 현재 상태 복원 가능
 * - 완벽한 감사 추적(Audit Trail) 제공
 */
sealed class OrderEvent {
    abstract val orderId: Long
    abstract val occurredAt: Instant
    abstract val eventType: String

    /**
     * 주문 생성 이벤트
     */
    data class OrderCreated(
        override val orderId: Long,
        val orderNumber: String,
        val userId: Long,
        val totalAmount: Long,
        val discountAmount: Long,
        val finalAmount: Long,
        val usedCouponId: Long?,
        val items: List<OrderItemSnapshot>,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent() {
        override val eventType: String = "OrderCreated"
    }

    /**
     * 주문 확정 이벤트 (결제 완료)
     */
    data class OrderConfirmed(
        override val orderId: Long,
        val paymentId: Long?,
        val confirmedBy: String = "SYSTEM",
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent() {
        override val eventType: String = "OrderConfirmed"
    }

    /**
     * 주문 완료 이벤트 (배송 완료 등)
     */
    data class OrderCompleted(
        override val orderId: Long,
        val completedBy: String = "SYSTEM",
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent() {
        override val eventType: String = "OrderCompleted"
    }

    /**
     * 주문 취소 이벤트
     */
    data class OrderCancelled(
        override val orderId: Long,
        val reason: String,
        val cancelledBy: String,
        val refundAmount: Long?,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent() {
        override val eventType: String = "OrderCancelled"
    }

    /**
     * 주문 실패 이벤트
     */
    data class OrderFailed(
        override val orderId: Long,
        val reason: String,
        val errorCode: String?,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent() {
        override val eventType: String = "OrderFailed"
    }
}

/**
 * 주문 아이템 스냅샷 (이벤트 저장용)
 *
 * 이벤트 발생 시점의 주문 아이템 정보를 불변으로 저장
 */
data class OrderItemSnapshot(
    val productId: Long,
    val productName: String,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val giftWrapPrice: Int = 0,
    val totalPrice: Int
) {
    companion object {
        fun from(
            productId: Long,
            productName: String,
            categoryName: String,
            quantity: Int,
            unitPrice: Int,
            giftWrap: Boolean = false,
            giftMessage: String? = null,
            giftWrapPrice: Int = 0
        ): OrderItemSnapshot {
            return OrderItemSnapshot(
                productId = productId,
                productName = productName,
                categoryName = categoryName,
                quantity = quantity,
                unitPrice = unitPrice,
                giftWrap = giftWrap,
                giftMessage = giftMessage,
                giftWrapPrice = giftWrapPrice,
                totalPrice = (unitPrice + giftWrapPrice) * quantity
            )
        }
    }
}
