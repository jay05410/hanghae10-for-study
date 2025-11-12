package io.hhplus.ecommerce.delivery.domain.constant

/**
 * 배송 상태
 *
 * 상태 전환 흐름:
 * PENDING(대기) → PREPARING(배송준비) → SHIPPED(발송완료) → DELIVERED(배송완료)
 *                                                           ↓
 *                                                        FAILED(배송실패)
 */
enum class DeliveryStatus {
    /** 배송 대기 (주문 생성 직후) */
    PENDING,

    /** 배송 준비 중 (제조 완료 후) */
    PREPARING,

    /** 발송 완료 (운송장 등록 완료) */
    SHIPPED,

    /** 배송 완료 */
    DELIVERED,

    /** 배송 실패 */
    FAILED;

    /**
     * 다음 상태로 전환 가능한지 검증
     */
    fun canTransitionTo(newStatus: DeliveryStatus): Boolean {
        val validTransitions = when (this) {
            PENDING -> listOf(PREPARING, FAILED)
            PREPARING -> listOf(SHIPPED, FAILED)
            SHIPPED -> listOf(DELIVERED, FAILED)
            DELIVERED -> emptyList()
            FAILED -> emptyList()
        }
        return newStatus in validTransitions
    }

    /**
     * 배송지 변경 가능한 상태인지 확인
     */
    fun canChangeAddress(): Boolean = this == PENDING

    /**
     * 반품 가능한 상태인지 확인
     */
    fun canReturn(): Boolean = this == DELIVERED
}
