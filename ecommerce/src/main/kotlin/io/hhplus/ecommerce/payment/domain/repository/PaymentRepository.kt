package io.hhplus.ecommerce.payment.domain.repository

import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.domain.entity.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByPaymentNumber(paymentNumber: String): Payment?
    fun findByOrderId(orderId: Long): List<Payment>
    fun findByUserId(userId: Long): List<Payment>
    fun findByStatus(status: PaymentStatus): List<Payment>
    fun findByExternalTransactionId(externalTransactionId: String): Payment?

    // FETCH JOIN 메서드들
    fun findPaymentWithHistoriesByPaymentNumber(paymentNumber: String): Payment?
    fun findPaymentsWithHistoriesByOrderId(orderId: Long): List<Payment>
    fun findPaymentWithHistoriesById(paymentId: Long): Payment?
}