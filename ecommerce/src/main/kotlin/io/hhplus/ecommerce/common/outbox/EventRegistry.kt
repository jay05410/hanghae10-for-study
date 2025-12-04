package io.hhplus.ecommerce.common.outbox

/**
 * 이벤트 레지스트리 - 모든 도메인 이벤트를 중앙에서 관리
 *
 * 목적:
 * - 이벤트 타입의 명시적 정의
 * - 이벤트 흐름의 가시성 확보
 * - 이벤트 핸들러 매핑의 중앙화
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
 *
 * 사용법:
 * 1. 새로운 이벤트 추가: EventTypes에 상수 정의
 * 2. 핸들러 등록: EventHandler 구현 및 @Component 등록
 * 3. 이벤트 발행: OutboxEventService.publishEvent() 사용
 */
object EventRegistry {

    /**
     * 이벤트 타입 정의
     *
     * 형식: {Domain}{Action}
     * - Order 도메인: OrderCreated, OrderCancelled, OrderConfirmed
     * - Payment 도메인: PaymentCompleted, PaymentFailed
     * - Inventory 도메인: StockDeducted, StockRestored (내부 이벤트)
     * - Coupon 도메인: CouponUsed, CouponRestored (내부 이벤트)
     */
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

    /**
     * 애그리게이트 타입 정의
     */
    object AggregateTypes {
        const val ORDER = "Order"
        const val PAYMENT = "Payment"
        const val INVENTORY = "Inventory"
        const val COUPON = "Coupon"
        const val POINT = "Point"
        const val DELIVERY = "Delivery"
        const val CART = "Cart"
    }

    /**
     * 이벤트 메타데이터
     * - 이벤트 타입별 설명, 발행자, 구독자 정보
     */
    data class EventMetadata(
        val eventType: String,
        val description: String,
        val publisher: String,
        val subscribers: List<String>
    )

    /**
     * 이벤트 카탈로그 - 모든 이벤트의 메타데이터
     *
     * Saga 흐름에 따른 이벤트 발행/구독 관계:
     * - OrderCreated: 주문 생성 → 결제 처리 트리거
     * - PaymentCompleted: 결제 완료 → 재고/포인트/쿠폰/배송/장바구니 처리
     * - PaymentFailed: 결제 실패 → 주문 상태 변경
     * - OrderCancelled: 주문 취소 → 재고 복구, 포인트 환불
     */
    val catalog: Map<String, EventMetadata> = mapOf(
        // === Saga Step 1: 주문 생성 ===
        EventTypes.ORDER_CREATED to EventMetadata(
            eventType = EventTypes.ORDER_CREATED,
            description = "주문이 생성되었을 때 발행. PaymentEventHandler가 결제를 처리함",
            publisher = "OrderCommandUseCase",
            subscribers = listOf("PaymentEventHandler")
        ),

        // === Saga Step 2: 결제 완료 (분기점) ===
        EventTypes.PAYMENT_COMPLETED to EventMetadata(
            eventType = EventTypes.PAYMENT_COMPLETED,
            description = "결제가 완료되었을 때 발행. 포인트 차감은 결제 시 처리됨. 재고 차감, 쿠폰 사용, 배송 생성, 장바구니 정리를 병렬로 트리거함",
            publisher = "PaymentEventHandler",
            subscribers = listOf(
                "InventoryEventHandler",  // 재고 차감
                "CouponEventHandler",      // 쿠폰 사용 처리
                "DeliveryEventHandler",    // 배송 생성
                "CartEventHandler"         // 장바구니 정리
            )
        ),

        // === 결제 실패 (보상 트랜잭션 트리거) ===
        EventTypes.PAYMENT_FAILED to EventMetadata(
            eventType = EventTypes.PAYMENT_FAILED,
            description = "결제가 실패했을 때 발행. 주문 상태를 PAYMENT_FAILED로 변경",
            publisher = "PaymentEventHandler",
            subscribers = listOf("OrderEventHandler")
        ),

        // === 주문 취소 (보상 트랜잭션) ===
        EventTypes.ORDER_CANCELLED to EventMetadata(
            eventType = EventTypes.ORDER_CANCELLED,
            description = "주문이 취소되었을 때 발행. 재고 복구, 포인트 환불을 트리거함",
            publisher = "OrderCommandUseCase",
            subscribers = listOf(
                "InventoryEventHandler",  // 재고 복구
                "PointEventHandler"        // 포인트 환불
            )
        ),

        // === 주문 확정 ===
        EventTypes.ORDER_CONFIRMED to EventMetadata(
            eventType = EventTypes.ORDER_CONFIRMED,
            description = "주문이 확정되었을 때 발행. 통계 기록 등 후처리를 트리거함",
            publisher = "OrderCommandUseCase",
            subscribers = listOf()
        )
    )

    /**
     * 이벤트 타입의 유효성 검증
     */
    fun isValidEventType(eventType: String): Boolean {
        return catalog.containsKey(eventType)
    }

    /**
     * 이벤트의 구독자 목록 조회
     */
    fun getSubscribers(eventType: String): List<String> {
        return catalog[eventType]?.subscribers ?: emptyList()
    }
}
