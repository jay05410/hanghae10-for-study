package io.hhplus.ecommerce.payment.domain.entity

import java.time.LocalDateTime

/**
 * 결제 이력 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 결제 상태 변경 이력 관리
 * - 결제 처리 과정 추적
 * - PG사 응답 데이터 보관
 *
 * 비즈니스 규칙:
 * - 모든 결제 상태 변경은 이력으로 기록되어야 함
 * - 결제 금액은 스냅샷으로 저장됨
 * - 결제 이력은 불변 객체로 관리
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/PaymentHistoryJpaEntity에서 처리됩니다.
 */
data class PaymentHistory(
    val id: Long = 0,
    val paymentId: Long,
    val statusBefore: String? = null,
    val statusAfter: String,
    val reason: String? = null,
    val pgResponse: String? = null,
    val amount: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    val updatedBy: Long = 0,
    val deletedAt: LocalDateTime? = null
) {

    /**
     * 결제 상태 유효성 검증
     */
    fun validateStatus() {
        require(statusAfter.isNotBlank()) { "변경 후 상태는 필수입니다" }
        require(statusAfter in listOf("PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED")) {
            "유효하지 않은 결제 상태입니다: $statusAfter"
        }
        if (statusBefore != null) {
            require(statusBefore in listOf("PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED")) {
                "유효하지 않은 이전 결제 상태입니다: $statusBefore"
            }
        }
    }

    /**
     * 삭제 여부 확인
     */
    fun isDeleted(): Boolean = deletedAt != null

    companion object {
        /**
         * 결제 이력 생성 팩토리 메서드
         *
         * @param paymentId 결제 ID
         * @param statusBefore 이전 상태
         * @param statusAfter 변경 후 상태
         * @param reason 변경 사유
         * @param pgResponse PG사 응답
         * @param amount 결제 금액
         * @param createdBy 생성자 ID
         * @return 생성된 PaymentHistory 도메인 모델
         * @throws IllegalArgumentException 유효하지 않은 입력 시
         */
        fun create(
            paymentId: Long,
            statusBefore: String? = null,
            statusAfter: String,
            reason: String? = null,
            pgResponse: String? = null,
            amount: Long,
            createdBy: Long
        ): PaymentHistory {
            require(paymentId > 0) { "결제 ID는 유효해야 합니다" }
            require(statusAfter.isNotBlank()) { "변경 후 상태는 필수입니다" }
            require(amount >= 0) { "결제 금액은 0 이상이어야 합니다" }

            val now = LocalDateTime.now()
            return PaymentHistory(
                paymentId = paymentId,
                statusBefore = statusBefore,
                statusAfter = statusAfter,
                reason = reason,
                pgResponse = pgResponse,
                amount = amount,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            ).also { it.validateStatus() }
        }

        /**
         * 최초 결제 이력 생성
         *
         * @param paymentId 결제 ID
         * @param amount 결제 금액
         * @param createdBy 생성자 ID
         * @param reason 생성 사유
         * @return 생성된 PaymentHistory 도메인 모델
         */
        fun createInitialHistory(
            paymentId: Long,
            amount: Long,
            createdBy: Long,
            reason: String? = "결제 생성"
        ): PaymentHistory {
            return create(
                paymentId = paymentId,
                statusBefore = null,
                statusAfter = "PENDING",
                reason = reason,
                amount = amount,
                createdBy = createdBy
            )
        }

        /**
         * 결제 상태 변경 이력 생성
         *
         * @param paymentId 결제 ID
         * @param statusBefore 이전 상태
         * @param statusAfter 변경 후 상태
         * @param amount 결제 금액
         * @param createdBy 생성자 ID
         * @param reason 변경 사유
         * @param pgResponse PG사 응답
         * @return 생성된 PaymentHistory 도메인 모델
         */
        fun createStatusChangeHistory(
            paymentId: Long,
            statusBefore: String,
            statusAfter: String,
            amount: Long,
            createdBy: Long,
            reason: String? = null,
            pgResponse: String? = null
        ): PaymentHistory {
            return create(
                paymentId = paymentId,
                statusBefore = statusBefore,
                statusAfter = statusAfter,
                reason = reason,
                pgResponse = pgResponse,
                amount = amount,
                createdBy = createdBy
            )
        }
    }
}