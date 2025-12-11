package io.hhplus.ecommerce.config.event

/**
 * 이벤트 레지스트리 - 모든 도메인 이벤트를 중앙에서 관리
 *
 * Saga 패턴 흐름:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  주문 생성 흐름 (Happy Path)                                                 │
 * │  ─────────────────────────────────────────────────────────────────────────  │
 * │  OrderCommandUseCase                                                        │
 * │       │                                                                     │
 * │       ▼                                                                     │
 * │  [OrderCreated] ──────► PaymentEventHandler                                 │
 * │                              │                                              │
 * │                              ▼                                              │
 * │                         [PaymentCompleted] ──► InventoryEventHandler        │
 * │                              │                      │ (재고 차감)            │
 * │                              │                                              │
 * │                              ├────────────────► PointEventHandler           │
 * │                              │                      │ (포인트 차감)          │
 * │                              │                                              │
 * │                              ├────────────────► CouponEventHandler          │
 * │                              │                      │ (쿠폰 사용 처리)       │
 * │                              │                                              │
 * │                              ├────────────────► DeliveryEventHandler        │
 * │                              │                      │ (배송 생성)            │
 * │                              │                                              │
 * │                              └────────────────► CartEventHandler            │
 * │                                                     │ (장바구니 정리)        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  주문 취소 흐름 (보상 트랜잭션)                                               │
 * │  ─────────────────────────────────────────────────────────────────────────  │
 * │  [OrderCancelled] ──────► InventoryEventHandler (재고 복구)                  │
 * │                     └───► PointEventHandler (포인트 환불)                    │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  결제 실패 흐름 (보상 트랜잭션)                                               │
 * │  ─────────────────────────────────────────────────────────────────────────  │
 * │  [PaymentFailed] ──────► OrderEventHandler (주문 상태 → PAYMENT_FAILED)      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
object EventRegistry {

    object EventTypes {
        // Order 도메인 이벤트 (Saga 시작점)
        const val ORDER_CREATED = "OrderCreated"
        const val ORDER_CANCELLED = "OrderCancelled"
        const val ORDER_CONFIRMED = "OrderConfirmed"

        // Payment 도메인 이벤트 (Saga 분기점)
        const val PAYMENT_COMPLETED = "PaymentCompleted"
        const val PAYMENT_FAILED = "PaymentFailed"

        // Inventory 도메인 이벤트 (내부용)
        const val STOCK_DEDUCTED = "StockDeducted"
        const val STOCK_RESTORED = "StockRestored"

        // Coupon 도메인 이벤트 (내부용)
        const val COUPON_USED = "CouponUsed"
        const val COUPON_RESTORED = "CouponRestored"

        // Point 도메인 이벤트 (내부용)
        const val POINT_USED = "PointUsed"
        const val POINT_REFUNDED = "PointRefunded"
    }

    object AggregateTypes {
        const val ORDER = "Order"
        const val PAYMENT = "Payment"
        const val INVENTORY = "Inventory"
        const val COUPON = "Coupon"
        const val POINT = "Point"
        const val DELIVERY = "Delivery"
        const val CART = "Cart"
    }

    data class EventMetadata(
        val eventType: String,
        val description: String,
        val publisher: String,
        val subscribers: List<String>
    )

    val catalog: Map<String, EventMetadata> = mapOf(
        EventTypes.ORDER_CREATED to EventMetadata(
            eventType = EventTypes.ORDER_CREATED,
            description = "주문이 생성되었을 때 발행. PaymentEventHandler가 결제를 처리함",
            publisher = "OrderCommandUseCase",
            subscribers = listOf("PaymentEventHandler")
        ),

        EventTypes.PAYMENT_COMPLETED to EventMetadata(
            eventType = EventTypes.PAYMENT_COMPLETED,
            description = "결제가 완료되었을 때 발행. 포인트 차감, 재고 차감, 쿠폰 사용, 배송 생성, 장바구니 정리, 판매 랭킹 업데이트, 데이터 플랫폼 전송을 병렬로 트리거함",
            publisher = "PaymentEventHandler",
            subscribers = listOf(
                "PointEventHandler",
                "InventoryEventHandler",
                "CouponEventHandler",
                "DeliveryEventHandler",
                "CartEventHandler",
                "ProductRankingEventHandler",
                "OrderDataPlatformHandler"
            )
        ),

        EventTypes.PAYMENT_FAILED to EventMetadata(
            eventType = EventTypes.PAYMENT_FAILED,
            description = "결제가 실패했을 때 발행. 주문 상태를 PAYMENT_FAILED로 변경",
            publisher = "PaymentEventHandler",
            subscribers = listOf("OrderEventHandler")
        ),

        EventTypes.ORDER_CANCELLED to EventMetadata(
            eventType = EventTypes.ORDER_CANCELLED,
            description = "주문이 취소되었을 때 발행. 재고 복구, 포인트 환불을 트리거함",
            publisher = "OrderCommandUseCase",
            subscribers = listOf(
                "InventoryEventHandler",
                "PointEventHandler"
            )
        ),

        EventTypes.ORDER_CONFIRMED to EventMetadata(
            eventType = EventTypes.ORDER_CONFIRMED,
            description = "주문이 확정되었을 때 발행. 통계 기록 등 후처리를 트리거함",
            publisher = "OrderCommandUseCase",
            subscribers = listOf()
        )
    )

    fun isValidEventType(eventType: String): Boolean {
        return catalog.containsKey(eventType)
    }

    fun getSubscribers(eventType: String): List<String> {
        return catalog[eventType]?.subscribers ?: emptyList()
    }
}
