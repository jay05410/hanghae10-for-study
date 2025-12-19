package io.hhplus.ecommerce.common.outbox.payload

import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import kotlinx.serialization.Serializable

/**
 * Outbox 이벤트 Payload 정의
 *
 * 모든 Outbox 이벤트의 payload를 타입 안전하게 관리
 * kotlinx.serialization을 사용하여 JSON 직렬화/역직렬화
 */

// ============================================
// Order 이벤트 Payloads
// ============================================

/**
 * 주문 생성 이벤트 Payload
 *
 * OrderCreated 이벤트 발행 시 사용
 * PaymentEventHandler에서 수신하여 결제 처리
 */
@Serializable
data class OrderCreatedPayload(
    val orderId: Long,
    val userId: Long,
    val orderNumber: String,
    val totalAmount: Long,
    val finalAmount: Long,
    val discountAmount: Long,
    val status: String,
    val usedCouponIds: List<Long> = emptyList(),
    val items: List<OrderCreatedItemPayload>,
    val deliveryAddress: DeliveryAddress
)

/**
 * 주문 생성 시 상품 항목 Payload
 *
 * 선물 포장 정보 포함
 */
@Serializable
data class OrderCreatedItemPayload(
    val productId: Long,
    val quantity: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null
)

/**
 * 주문 취소 이벤트 Payload
 *
 * OrderCancelled 이벤트 발행 시 사용
 * - 재고 복구 핸들러
 * - 포인트 환불 핸들러
 * - 쿠폰 복구 핸들러
 * - 장바구니 복구 핸들러
 */
@Serializable
data class OrderCancelledPayload(
    val orderId: Long,
    val userId: Long,
    val orderNumber: String,
    val finalAmount: Long,
    val reason: String,
    val status: String,
    val usedCouponIds: List<Long> = emptyList(),
    val items: List<OrderItemPayloadSimple>
)

/**
 * 주문 확정 이벤트 Payload
 *
 * OrderConfirmed 이벤트 발행 시 사용
 * - CouponEventHandler: 쿠폰 USED 확정
 * - 통계 기록 핸들러: 판매 통계
 */
@Serializable
data class OrderConfirmedPayload(
    val orderId: Long,
    val userId: Long,
    val orderNumber: String,
    val status: String,
    val usedCouponIds: List<Long> = emptyList(),
    val items: List<OrderItemPayloadSimple>
)

/**
 * 단순 상품 항목 Payload
 *
 * productId와 quantity만 포함
 * 취소/확정/결제 실패 등의 이벤트에서 사용
 */
@Serializable
data class OrderItemPayloadSimple(
    val productId: Long,
    val quantity: Int
)

// ============================================
// Payment 이벤트 Payloads
// ============================================

/**
 * 결제 완료 이벤트 Payload
 *
 * PaymentCompleted 이벤트 발행 시 사용
 *
 * 도메인 분리 원칙:
 * - Payment 도메인은 결제 정보만 발행
 * - 각 핸들러가 orderId로 자기 도메인 데이터 조회
 *   - InventoryEventHandler: stock_reservations 조회 → 재고 확정
 *   - OrderEventHandler: orders 조회 → 상태 변경
 *   - CouponEventHandler: orders에서 usedCouponId 조회
 *   - DeliveryEventHandler: orders + 배송지 조회
 */
@Serializable
data class PaymentCompletedPayload(
    val orderId: Long,
    val userId: Long,
    val paymentId: Long,
    val amount: Long
)

/**
 * 결제 실패 이벤트 Payload
 *
 * PaymentFailed 이벤트 발행 시 사용
 * 보상 트랜잭션 핸들러에서 수신
 */
@Serializable
data class PaymentFailedPayload(
    val orderId: Long,
    val userId: Long,
    val reason: String,
    val items: List<OrderCreatedItemPayload>
)

/**
 * 재고 부족 이벤트 Payload
 *
 * InventoryInsufficient 이벤트 발행 시 사용
 * 주문 보상 핸들러에서 수신하여 주문 취소 처리
 */
@Serializable
data class InventoryInsufficientPayload(
    val orderId: Long,
    val userId: Long,
    val productId: Long,
    val requestedQuantity: Int,
    val availableQuantity: Int,
    val reason: String
)

// ============================================
// Coupon 이벤트 Payloads
// ============================================

/**
 * 쿠폰 발급 요청 Payload
 *
 * 선착순 쿠폰 발급 요청 시 Kafka로 발행
 * CouponIssueConsumer에서 수신하여 실제 발급 처리
 */
@Serializable
data class CouponIssueRequestPayload(
    val couponId: Long,
    val userId: Long,
    val couponName: String,
    val requestedAt: Long
)
