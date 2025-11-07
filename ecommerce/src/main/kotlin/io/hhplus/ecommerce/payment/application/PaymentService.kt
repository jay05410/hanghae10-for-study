package io.hhplus.ecommerce.payment.application

import io.hhplus.ecommerce.payment.domain.*
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.common.exception.payment.PaymentException
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 결제 도메인의 핵심 비즈니스 로직 처리
 * - 다양한 결제 수단 및 방식 관리
 * - 결제 상태 및 이력 관리
 *
 * 책임:
 * - 결제 요청 처리 및 결과 관리
 * - 결제 성공/실패 상태 처리
 * - 사용자별 결제 내역 조회
 */
@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val snowflakeGenerator: SnowflakeGenerator
) {

    @Transactional
    fun processPayment(userId: Long, orderId: Long, amount: Long, paymentMethod: PaymentMethod = PaymentMethod.BALANCE): Payment {
        // 결제 엔티티 생성
        val paymentNumber = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.PAYMENT)
        val payment = Payment.create(
            paymentNumber = paymentNumber,
            userId = userId,
            orderId = orderId,
            amount = amount,
            paymentMethod = paymentMethod,
            createdBy = userId
        )

        val savedPayment = paymentRepository.save(payment)

        try {
            when (paymentMethod) {
                PaymentMethod.BALANCE -> {
                    // 잔액 결제는 별도의 서비스에서 처리
                    // 여기서는 결제 엔티티만 관리
                }
                else -> throw PaymentException.UnsupportedPaymentMethod(paymentMethod.name)
            }

            // 결제 완료 처리
            savedPayment.complete(
                completedBy = userId,
                externalTxId = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.TRANSACTION)
            )

            return paymentRepository.save(savedPayment)

        } catch (e: Exception) {
            // 결제 실패 처리
            savedPayment.fail(userId, e.message ?: "결제 처리 중 오류가 발생했습니다")
            paymentRepository.save(savedPayment)
            throw e
        }
    }


    fun getPayment(paymentId: Long): Payment? {
        return paymentRepository.findById(paymentId)
    }

    fun getPaymentsByUser(userId: Long): List<Payment> {
        return paymentRepository.findByUserId(userId)
    }
}