package io.hhplus.ecommerce.payment.domain.entity

import io.hhplus.ecommerce.payment.exception.PaymentException
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import java.time.LocalDateTime

/**
 * 결제 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 결제 정보 관리
 * - 결제 상태 전환 및 검증
 * - 결제 처리 및 취소 로직
 *
 * 비즈니스 규칙:
 * - 결제번호는 유일해야 함
 * - 결제 상태 전환은 정해진 규칙을 따라야 함
 * - 결제 정보는 불변 객체로 관리 (copy()로 변경)
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/PaymentJpaEntity에서 처리됩니다.
 */
data class Payment(
    val id: Long = 0,
    val paymentNumber: String,
    val orderId: Long,
    val userId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    var status: PaymentStatus = PaymentStatus.PENDING,
    var externalTransactionId: String? = null,
    var failureReason: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0,
    var deletedAt: LocalDateTime? = null
) {
    /**
     * 결제를 처리 중 상태로 전환
     *
     * @param processedBy 처리자 ID
     */
    fun process(processedBy: Long) {
        validateStatusTransition(PaymentStatus.PROCESSING)
        this.status = PaymentStatus.PROCESSING
        this.updatedBy = processedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제를 완료 상태로 전환
     *
     * @param completedBy 완료 처리자 ID
     * @param externalTxId 외부 거래 ID
     */
    fun complete(completedBy: Long, externalTxId: String? = null) {
        validateStatusTransition(PaymentStatus.COMPLETED)
        this.status = PaymentStatus.COMPLETED
        if (externalTxId != null) {
            this.externalTransactionId = externalTxId
        }
        this.updatedBy = completedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제를 실패 상태로 전환
     *
     * @param failedBy 실패 처리자 ID
     * @param reason 실패 사유
     */
    fun fail(failedBy: Long, reason: String) {
        validateStatusTransition(PaymentStatus.FAILED)
        this.status = PaymentStatus.FAILED
        this.failureReason = reason
        this.updatedBy = failedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제를 취소 상태로 전환
     *
     * @param cancelledBy 취소 처리자 ID
     */
    fun cancel(cancelledBy: Long) {
        if (!canBeCancelled()) {
            throw PaymentException.PaymentCancellationNotAllowed(paymentNumber, status)
        }

        this.status = PaymentStatus.CANCELLED
        this.updatedBy = cancelledBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제 완료 여부 확인
     */
    fun isCompleted(): Boolean = status == PaymentStatus.COMPLETED

    /**
     * 결제 취소 가능 여부 확인
     */
    fun canBeCancelled(): Boolean = status in listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING)

    /**
     * 결제 상태 전환 유효성 검증
     */
    private fun validateStatusTransition(newStatus: PaymentStatus) {
        val validTransitions = when (status) {
            PaymentStatus.PENDING -> listOf(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED, PaymentStatus.FAILED)
            PaymentStatus.PROCESSING -> listOf(PaymentStatus.COMPLETED, PaymentStatus.FAILED)
            PaymentStatus.COMPLETED -> emptyList()
            PaymentStatus.CANCELLED -> emptyList()
            PaymentStatus.FAILED -> emptyList()
        }

        if (newStatus !in validTransitions) {
            throw PaymentException.InvalidPaymentStatus(paymentNumber, status, newStatus)
        }
    }

    /**
     * 삭제 여부 확인
     */
    fun isDeleted(): Boolean = deletedAt != null

    companion object {
        /**
         * 결제 생성 팩토리 메서드
         *
         * @param paymentNumber 결제번호
         * @param orderId 주문 ID
         * @param userId 사용자 ID
         * @param amount 결제 금액
         * @param paymentMethod 결제 수단
         * @param createdBy 생성자 ID
         * @param externalTransactionId 외부 거래 ID
         * @return 생성된 Payment 도메인 모델
         * @throws IllegalArgumentException 유효하지 않은 입력 시
         */
        fun create(
            paymentNumber: String,
            orderId: Long,
            userId: Long,
            amount: Long,
            paymentMethod: PaymentMethod,
            createdBy: Long,
            externalTransactionId: String? = null
        ): Payment {
            require(paymentNumber.isNotBlank()) { "결제번호는 필수입니다" }
            require(orderId > 0) { "주문 ID는 유효해야 합니다" }
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(amount > 0) { "결제 금액은 0보다 커야 합니다" }

            val now = LocalDateTime.now()
            return Payment(
                paymentNumber = paymentNumber,
                orderId = orderId,
                userId = userId,
                amount = amount,
                paymentMethod = paymentMethod,
                externalTransactionId = externalTransactionId,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

