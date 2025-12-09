package io.hhplus.ecommerce.order.domain.eventsourcing

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.exception.OrderException
import java.time.Instant

/**
 * 주문 Aggregate (Event Sourcing)
 *
 * 이벤트 소싱 패턴에서 주문의 현재 상태를 관리하는 Aggregate Root
 *
 * 특징:
 * - 이벤트를 통해서만 상태 변경 (apply)
 * - 이벤트 목록으로부터 현재 상태 재구성 가능 (rebuild)
 * - 낙관적 잠금을 위한 version 관리
 * - 명령(Command) 처리 시 이벤트 생성 및 적용
 */
class OrderAggregate private constructor() {

    var id: Long = 0L
        private set
    var orderNumber: String = ""
        private set
    var userId: Long = 0L
        private set
    var totalAmount: Long = 0L
        private set
    var discountAmount: Long = 0L
        private set
    var finalAmount: Long = 0L
        private set
    var usedCouponId: Long? = null
        private set
    var status: OrderStatus = OrderStatus.PENDING
        private set
    var items: List<OrderItemSnapshot> = emptyList()
        private set
    var version: Int = 0
        private set

    // 미발행 이벤트 (저장 대기 중)
    private val _uncommittedEvents = mutableListOf<OrderEvent>()
    val uncommittedEvents: List<OrderEvent> get() = _uncommittedEvents.toList()

    companion object {
        /**
         * 새 주문 생성 (Command: CreateOrder)
         */
        fun create(
            orderId: Long,
            orderNumber: String,
            userId: Long,
            totalAmount: Long,
            discountAmount: Long = 0,
            usedCouponId: Long? = null,
            items: List<OrderItemSnapshot>
        ): OrderAggregate {
            require(orderNumber.isNotBlank()) { "주문번호는 필수입니다" }
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(totalAmount > 0) { "총 금액은 0보다 커야 합니다" }
            require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다" }
            require(items.isNotEmpty()) { "주문 아이템은 최소 1개 이상이어야 합니다" }

            val finalAmount = totalAmount - discountAmount
            require(finalAmount >= 0) { "최종 금액은 0 이상이어야 합니다" }

            val aggregate = OrderAggregate()
            val event = OrderEvent.OrderCreated(
                orderId = orderId,
                orderNumber = orderNumber,
                userId = userId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                usedCouponId = usedCouponId,
                items = items
            )

            aggregate.applyAndTrack(event)
            return aggregate
        }

        /**
         * 이벤트 목록으로부터 Aggregate 재구성
         */
        fun rebuild(events: List<OrderEvent>): OrderAggregate {
            require(events.isNotEmpty()) { "이벤트 목록이 비어있습니다" }

            val aggregate = OrderAggregate()
            events.forEach { event ->
                aggregate.apply(event)
                aggregate.version++
            }
            return aggregate
        }

        /**
         * 스냅샷 + 이후 이벤트로 Aggregate 재구성
         */
        fun fromSnapshot(snapshot: OrderSnapshot, events: List<OrderEvent>): OrderAggregate {
            val aggregate = OrderAggregate()
            aggregate.restoreFromSnapshot(snapshot)
            events.forEach { event ->
                aggregate.apply(event)
                aggregate.version++
            }
            return aggregate
        }
    }

    /**
     * 주문 확정 (Command: ConfirmOrder)
     */
    fun confirm(paymentId: Long? = null, confirmedBy: String = "SYSTEM"): OrderEvent {
        validateStatusTransition(OrderStatus.CONFIRMED)

        val event = OrderEvent.OrderConfirmed(
            orderId = id,
            paymentId = paymentId,
            confirmedBy = confirmedBy
        )
        applyAndTrack(event)
        return event
    }

    /**
     * 주문 완료 (Command: CompleteOrder)
     */
    fun complete(completedBy: String = "SYSTEM"): OrderEvent {
        validateStatusTransition(OrderStatus.COMPLETED)

        val event = OrderEvent.OrderCompleted(
            orderId = id,
            completedBy = completedBy
        )
        applyAndTrack(event)
        return event
    }

    /**
     * 주문 취소 (Command: CancelOrder)
     */
    fun cancel(reason: String, cancelledBy: String, refundAmount: Long? = null): OrderEvent {
        if (!status.canBeCancelled()) {
            throw OrderException.OrderCancellationNotAllowed(orderNumber, status)
        }

        val event = OrderEvent.OrderCancelled(
            orderId = id,
            reason = reason,
            cancelledBy = cancelledBy,
            refundAmount = refundAmount ?: finalAmount
        )
        applyAndTrack(event)
        return event
    }

    /**
     * 주문 실패 (Command: FailOrder)
     */
    fun fail(reason: String, errorCode: String? = null): OrderEvent {
        val event = OrderEvent.OrderFailed(
            orderId = id,
            reason = reason,
            errorCode = errorCode
        )
        applyAndTrack(event)
        return event
    }

    /**
     * 이벤트 적용 (상태 변경) - 내부용
     */
    private fun apply(event: OrderEvent) {
        when (event) {
            is OrderEvent.OrderCreated -> {
                this.id = event.orderId
                this.orderNumber = event.orderNumber
                this.userId = event.userId
                this.totalAmount = event.totalAmount
                this.discountAmount = event.discountAmount
                this.finalAmount = event.finalAmount
                this.usedCouponId = event.usedCouponId
                this.items = event.items
                this.status = OrderStatus.PENDING
            }
            is OrderEvent.OrderConfirmed -> {
                this.status = OrderStatus.CONFIRMED
            }
            is OrderEvent.OrderCompleted -> {
                this.status = OrderStatus.COMPLETED
            }
            is OrderEvent.OrderCancelled -> {
                this.status = OrderStatus.CANCELLED
            }
            is OrderEvent.OrderFailed -> {
                this.status = OrderStatus.FAILED
            }
        }
    }

    /**
     * 이벤트 적용 + 미발행 이벤트 목록에 추가
     */
    private fun applyAndTrack(event: OrderEvent) {
        apply(event)
        version++
        _uncommittedEvents.add(event)
    }

    /**
     * 미발행 이벤트 목록 초기화 (저장 완료 후 호출)
     */
    fun clearUncommittedEvents() {
        _uncommittedEvents.clear()
    }

    /**
     * 스냅샷 생성
     */
    fun toSnapshot(): OrderSnapshot {
        return OrderSnapshot(
            id = id,
            orderNumber = orderNumber,
            userId = userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            usedCouponId = usedCouponId,
            status = status,
            items = items,
            version = version,
            snapshotAt = Instant.now()
        )
    }

    /**
     * 스냅샷으로부터 상태 복원
     */
    private fun restoreFromSnapshot(snapshot: OrderSnapshot) {
        this.id = snapshot.id
        this.orderNumber = snapshot.orderNumber
        this.userId = snapshot.userId
        this.totalAmount = snapshot.totalAmount
        this.discountAmount = snapshot.discountAmount
        this.finalAmount = snapshot.finalAmount
        this.usedCouponId = snapshot.usedCouponId
        this.status = snapshot.status
        this.items = snapshot.items
        this.version = snapshot.version
    }

    private fun validateStatusTransition(newStatus: OrderStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw OrderException.InvalidOrderStatus(orderNumber, status, newStatus)
        }
    }

    // 상태 확인 헬퍼 메서드
    fun canBeCancelled(): Boolean = status.canBeCancelled()
    fun isPaid(): Boolean = status.isPaid()
    fun isCompleted(): Boolean = status.isCompleted()
}

/**
 * 주문 스냅샷 (성능 최적화용)
 *
 * 이벤트가 많이 쌓인 경우 스냅샷을 저장하여
 * 전체 이벤트 재생 없이 빠르게 상태 복원
 */
data class OrderSnapshot(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
    val usedCouponId: Long?,
    val status: OrderStatus,
    val items: List<OrderItemSnapshot>,
    val version: Int,
    val snapshotAt: Instant
)
