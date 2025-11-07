package io.hhplus.ecommerce.payment.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.common.exception.payment.PaymentException
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
// import jakarta.persistence.*

// @Entity
// @Table(name = "payments")
class Payment(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true, length = 50)
    val paymentNumber: String,

    // @Column(nullable = false)
    val orderId: Long,

    // @Column(nullable = false)
    val userId: Long,

    // @Column(nullable = false)
    val amount: Long,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    val paymentMethod: PaymentMethod,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING,

    // @Column(length = 100)
    val externalTransactionId: String? = null,

    // @Column(columnDefinition = "TEXT")
    var failureReason: String? = null
) : ActiveJpaEntity() {
    fun process(processedBy: Long) {
        validateStatusTransition(PaymentStatus.PROCESSING)
        this.status = PaymentStatus.PROCESSING
    }

    fun complete(completedBy: Long, externalTxId: String? = null) {
        validateStatusTransition(PaymentStatus.COMPLETED)
        this.status = PaymentStatus.COMPLETED

        if (externalTxId != null && paymentMethod != PaymentMethod.BALANCE) {
            // externalTransactionId는 불변이므로 새 객체 생성 필요시에만 업데이트
        }
    }

    fun fail(failedBy: Long, reason: String) {
        validateStatusTransition(PaymentStatus.FAILED)
        this.status = PaymentStatus.FAILED
        this.failureReason = reason
    }

    fun cancel(cancelledBy: Long) {
        if (!canBeCancelled()) {
            throw PaymentException.PaymentCancellationNotAllowed(paymentNumber, status)
        }

        this.status = PaymentStatus.CANCELLED
    }

    fun isCompleted(): Boolean = status == PaymentStatus.COMPLETED

    fun canBeCancelled(): Boolean = status in listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING)

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

    companion object {
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

            return Payment(
                paymentNumber = paymentNumber,
                orderId = orderId,
                userId = userId,
                amount = amount,
                paymentMethod = paymentMethod,
                externalTransactionId = externalTransactionId
            )
        }
    }
}

