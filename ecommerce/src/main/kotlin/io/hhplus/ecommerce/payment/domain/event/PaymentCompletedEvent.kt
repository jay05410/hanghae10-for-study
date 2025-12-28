package io.hhplus.ecommerce.payment.domain.event

/**
 * 결제 완료 도메인 이벤트
 *
 * Spring ApplicationEventPublisher를 통해 발행되며,
 * @TransactionalEventListener(phase = AFTER_COMMIT)로 트랜잭션 커밋 후 처리
 */
data class PaymentCompletedEvent(
    val orderId: Long,
    val userId: Long,
    val paymentId: Long,
    val amount: Long
)
