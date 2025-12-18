package io.hhplus.ecommerce.checkout.domain.model

import io.hhplus.ecommerce.checkout.application.usecase.CheckoutItem
import java.time.LocalDateTime

/**
 * 체크아웃 세션 - 도메인 모델
 *
 * 역할:
 * - 주문하기 시 생성되는 세션 정보
 * - 주문 ID, 만료 시간, 금액 정보, 상품 목록 포함
 *
 * 라이프사이클:
 * - 주문하기 클릭 시 생성
 * - 결제 완료 또는 만료 시 소멸
 */
data class CheckoutSession(
    val orderId: Long,
    val orderNumber: String,
    val userId: Long,
    val expiresAt: LocalDateTime,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
    val reservationIds: List<Long>,
    val items: List<CheckoutItem>
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun getRemainingSeconds(): Long {
        val now = LocalDateTime.now()
        return if (now.isBefore(expiresAt)) {
            java.time.Duration.between(now, expiresAt).seconds
        } else {
            0
        }
    }
}
