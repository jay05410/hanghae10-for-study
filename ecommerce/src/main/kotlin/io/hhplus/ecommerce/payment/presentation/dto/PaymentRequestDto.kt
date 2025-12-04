package io.hhplus.ecommerce.payment.presentation.dto

import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결제 처리 요청")
data class ProcessPaymentRequest(
    @Schema(description = "사용자 ID", example = "1", required = true)
    val userId: Long,

    @Schema(description = "주문 ID", example = "100", required = true)
    val orderId: Long,

    @Schema(description = "결제 금액", example = "50000", required = true)
    val amount: Long,

    @Schema(description = "결제 수단", example = "BALANCE", defaultValue = "BALANCE")
    val paymentMethod: PaymentMethod = PaymentMethod.BALANCE
)

@Schema(description = "잔액 충전 요청")
data class ChargeBalanceRequest(
    @Schema(description = "사용자 ID", example = "1", required = true)
    val userId: Long,

    @Schema(description = "충전 금액", example = "100000", required = true)
    val amount: Long,

    @Schema(description = "충전 설명 (선택)", example = "포인트 충전")
    val description: String? = null
)
