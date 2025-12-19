package io.hhplus.ecommerce.common.sse

import java.time.LocalDateTime

/**
 * SSE 알림 데이터 전송 객체
 */
data class SseNotification(
    val type: String,
    val title: String,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * 쿠폰 발급 알림
 */
data class CouponIssuedNotification(
    val couponId: Long,
    val couponName: String,
    val message: String = "쿠폰이 발급되었습니다",
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * 주문 완료 알림
 */
data class OrderCompletedNotification(
    val orderId: Long,
    val orderNumber: String,
    val message: String = "주문이 완료되었습니다",
    val totalAmount: Long,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * 결제 완료 알림
 */
data class PaymentCompletedNotification(
    val paymentId: Long,
    val orderId: Long,
    val message: String = "결제가 완료되었습니다",
    val amount: Long,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
