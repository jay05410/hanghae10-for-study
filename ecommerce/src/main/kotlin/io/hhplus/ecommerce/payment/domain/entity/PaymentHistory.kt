package io.hhplus.ecommerce.payment.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*

// @Entity
// @Table(name = "payment_history")
class PaymentHistory(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val paymentId: Long,

    // @Column(length = 20)
    val statusBefore: String? = null,

    // @Column(nullable = false, length = 20)
    val statusAfter: String,

    // @Column(length = 500)
    val reason: String? = null,

    // @Column(columnDefinition = "JSON")
    val pgResponse: String? = null, // PG사 응답을 JSON 형태로 저장

    // @Column(nullable = false)
    val amount: Int // 결제 금액 스냅샷
) : ActiveJpaEntity() {

    fun validateStatus() {
        require(statusAfter.isNotBlank()) { "변경 후 상태는 필수입니다" }
        require(statusAfter in listOf("PENDING", "COMPLETED", "FAILED", "CANCELLED")) {
            "유효하지 않은 결제 상태입니다: $statusAfter"
        }
        if (statusBefore != null) {
            require(statusBefore in listOf("PENDING", "COMPLETED", "FAILED", "CANCELLED")) {
                "유효하지 않은 이전 결제 상태입니다: $statusBefore"
            }
        }
    }

    companion object {
        fun create(
            paymentId: Long,
            statusBefore: String? = null,
            statusAfter: String,
            reason: String? = null,
            pgResponse: String? = null,
            amount: Int
        ): PaymentHistory {
            require(paymentId > 0) { "결제 ID는 유효해야 합니다" }
            require(statusAfter.isNotBlank()) { "변경 후 상태는 필수입니다" }
            require(amount >= 0) { "결제 금액은 0 이상이어야 합니다" }

            return PaymentHistory(
                paymentId = paymentId,
                statusBefore = statusBefore,
                statusAfter = statusAfter,
                reason = reason,
                pgResponse = pgResponse,
                amount = amount
            ).also { it.validateStatus() }
        }

        fun createInitialHistory(
            paymentId: Long,
            amount: Int,
            reason: String? = "결제 생성"
        ): PaymentHistory {
            return create(
                paymentId = paymentId,
                statusBefore = null,
                statusAfter = "PENDING",
                reason = reason,
                amount = amount
            )
        }

        fun createStatusChangeHistory(
            paymentId: Long,
            statusBefore: String,
            statusAfter: String,
            amount: Int,
            reason: String? = null,
            pgResponse: String? = null
        ): PaymentHistory {
            return create(
                paymentId = paymentId,
                statusBefore = statusBefore,
                statusAfter = statusAfter,
                reason = reason,
                pgResponse = pgResponse,
                amount = amount
            )
        }
    }
}