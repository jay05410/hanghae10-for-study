package io.hhplus.ecommerce.payment.dto

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import java.time.LocalDateTime

/**
 * 결제 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - Payment 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
data class PaymentResponse(
    val id: Long,
    val paymentNumber: String,
    val orderId: Long,
    val userId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val externalTransactionId: String?,
    val failureReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun Payment.toResponse(): PaymentResponse = PaymentResponse(
            id = this.id,
            paymentNumber = this.paymentNumber,
            orderId = this.orderId,
            userId = this.userId,
            amount = this.amount,
            paymentMethod = this.paymentMethod,
            status = this.status,
            externalTransactionId = this.externalTransactionId,
            failureReason = this.failureReason,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}