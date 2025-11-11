package io.hhplus.ecommerce.payment.dto

import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod

data class ProcessPaymentRequest(
    val userId: Long,
    val orderId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.BALANCE
)

data class ChargeBalanceRequest(
    val userId: Long,
    val amount: Long,
    val description: String? = null
)