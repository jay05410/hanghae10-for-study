package io.hhplus.ecommerce.order.domain.constant

/**
 * 주문 상태
 *
 * 상태 흐름:
 * - PENDING_PAYMENT: 체크아웃 진입, 결제 대기 중 (사용자에게 미노출)
 * - PENDING: 결제 완료, 처리 대기
 * - CONFIRMED: 주문 확정
 * - COMPLETED: 배송 완료
 * - CANCELLED: 취소
 * - FAILED: 결제 실패
 * - EXPIRED: 결제 시간 초과 (사용자에게 미노출)
 */
enum class OrderStatus {
    PENDING_PAYMENT,
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    FAILED,
    EXPIRED;

    /**
     * 사용자에게 노출되는 상태인지 확인
     * PENDING_PAYMENT, EXPIRED는 내부 처리용으로 사용자에게 노출되지 않음
     */
    fun isVisibleToUser(): Boolean = this in listOf(PENDING, CONFIRMED, COMPLETED, CANCELLED)

    /**
     * 결제 대기 상태인지 확인
     */
    fun isAwaitingPayment(): Boolean = this == PENDING_PAYMENT

    fun canBeCancelled(): Boolean = this in listOf(PENDING_PAYMENT, PENDING, CONFIRMED)

    fun isPaid(): Boolean = this in listOf(PENDING, CONFIRMED, COMPLETED)

    fun isCompleted(): Boolean = this == COMPLETED

    fun canTransitionTo(newStatus: OrderStatus): Boolean {
        val validTransitions = when (this) {
            PENDING_PAYMENT -> listOf(PENDING, CONFIRMED, CANCELLED, FAILED, EXPIRED)  // 결제 완료 시 바로 CONFIRMED 가능
            PENDING -> listOf(CONFIRMED, CANCELLED, FAILED)
            CONFIRMED -> listOf(COMPLETED, CANCELLED)
            COMPLETED -> emptyList()
            CANCELLED -> emptyList()
            FAILED -> emptyList()
            EXPIRED -> emptyList()
        }
        return newStatus in validTransitions
    }
}