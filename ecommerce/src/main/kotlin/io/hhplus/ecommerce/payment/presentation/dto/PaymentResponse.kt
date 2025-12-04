package io.hhplus.ecommerce.payment.presentation.dto

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 결제 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - Payment 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
@Schema(description = "결제 정보")
data class PaymentResponse(
    @Schema(description = "결제 ID", example = "1")
    val id: Long,

    @Schema(description = "결제 번호", example = "PAY-20250113-001")
    val paymentNumber: String,

    @Schema(description = "주문 ID", example = "100")
    val orderId: Long,

    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "결제 금액", example = "50000")
    val amount: Long,

    @Schema(description = "결제 수단", example = "BALANCE")
    val paymentMethod: PaymentMethod,

    @Schema(description = "결제 상태", example = "COMPLETED")
    val status: PaymentStatus,

    @Schema(description = "외부 거래 ID (선택)", example = "TXN-123456")
    val externalTransactionId: String?,

    @Schema(description = "실패 사유 (선택)", example = "잔액 부족")
    val failureReason: String?
)

fun Payment.toResponse(): PaymentResponse = PaymentResponse(
    id = this.id,
    paymentNumber = this.paymentNumber,
    orderId = this.orderId,
    userId = this.userId,
    amount = this.amount,
    paymentMethod = this.paymentMethod,
    status = this.status,
    externalTransactionId = this.externalTransactionId,
    failureReason = this.failureReason
)
