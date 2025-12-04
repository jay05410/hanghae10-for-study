package io.hhplus.ecommerce.payment.domain.service

import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.model.RefundResult
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.payment.exception.PaymentException
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 결제 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 결제 엔티티 생성 및 상태 관리
 * - 중복 결제 검증
 * - 결제 결과에 따른 상태 전환
 *
 * 책임:
 * - 결제 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - 외부 시스템(PG, 포인트 등) 연동 로직 없음
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class PaymentDomainService(
    private val paymentRepository: PaymentRepository,
    private val snowflakeGenerator: SnowflakeGenerator
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 중복 결제 검증
     *
     * @param orderId 주문 ID
     * @throws PaymentException.DuplicatePayment 이미 결제가 존재하는 경우
     */
    fun validateNoDuplicatePayment(orderId: Long) {
        val existingPayment = paymentRepository.findByOrderId(orderId).firstOrNull()
        if (existingPayment != null) {
            throw PaymentException.DuplicatePayment(
                "주문 ID ${orderId}는 이미 결제 처리되었습니다. 기존 결제 ID: ${existingPayment.id}"
            )
        }
    }

    /**
     * 결제 엔티티 생성 (PENDING 상태)
     *
     * @param userId 사용자 ID
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @param paymentMethod 결제 수단
     * @return 생성된 결제 엔티티 (저장됨)
     */
    fun createPayment(
        userId: Long,
        orderId: Long,
        amount: Long,
        paymentMethod: PaymentMethod
    ): Payment {
        val paymentNumber = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.PAYMENT)
        val payment = Payment.create(
            paymentNumber = paymentNumber,
            userId = userId,
            orderId = orderId,
            amount = amount,
            paymentMethod = paymentMethod
        )
        return paymentRepository.save(payment)
    }

    /**
     * 결제 상태를 PROCESSING으로 전환
     *
     * @param payment 결제 엔티티
     * @return 상태 전환된 결제 엔티티 (저장됨)
     */
    fun markAsProcessing(payment: Payment): Payment {
        payment.process()
        return paymentRepository.save(payment)
    }

    /**
     * 결제 결과에 따른 상태 처리
     *
     * @param payment 결제 엔티티
     * @param result 결제 실행 결과
     * @return 최종 상태의 결제 엔티티
     * @throws PaymentException.PaymentProcessingError 결제 실패 시
     */
    fun handlePaymentResult(payment: Payment, result: PaymentResult): Payment {
        return if (result.success) {
            payment.complete(result.externalTransactionId)
            val completedPayment = paymentRepository.save(payment)
            logger.info("결제 완료: paymentId=${completedPayment.id}, orderId=${payment.orderId}")
            completedPayment
        } else {
            payment.fail(result.failureReason ?: "결제 실패")
            paymentRepository.save(payment)
            logger.warn("결제 실패: orderId=${payment.orderId}, reason=${result.failureReason}")
            throw PaymentException.PaymentProcessingError(result.failureReason ?: "결제 처리 실패")
        }
    }

    /**
     * 결제 가능 조건 미충족 시 실패 처리
     *
     * @param payment 결제 엔티티
     * @param reason 실패 사유
     */
    fun markAsFailed(payment: Payment, reason: String) {
        payment.fail(reason)
        paymentRepository.save(payment)
    }

    /**
     * 결제 ID로 조회
     *
     * @param paymentId 결제 ID
     * @return 결제 엔티티
     * @throws PaymentException.PaymentProcessingError 결제 정보가 없는 경우
     */
    fun getPayment(paymentId: Long): Payment {
        return paymentRepository.findById(paymentId)
            ?: throw PaymentException.PaymentProcessingError("결제 정보를 찾을 수 없습니다: $paymentId")
    }

    /**
     * 환불 가능 여부 검증
     *
     * @param payment 결제 엔티티
     * @throws PaymentException.PaymentProcessingError 환불 불가능한 상태인 경우
     */
    fun validateRefundable(payment: Payment) {
        if (!payment.isCompleted()) {
            throw PaymentException.PaymentProcessingError(
                "완료된 결제만 환불할 수 있습니다. 현재 상태: ${payment.status}"
            )
        }
    }

    /**
     * 환불 결과 처리
     *
     * @param payment 결제 엔티티
     * @param result 환불 실행 결과
     * @return 환불 결과
     */
    fun handleRefundResult(payment: Payment, result: RefundResult): RefundResult {
        if (result.success) {
            payment.refund()
            paymentRepository.save(payment)
            logger.info("환불 완료: paymentId=${payment.id}, refundedAmount=${result.refundedAmount}")
        } else {
            logger.warn("환불 실패: paymentId=${payment.id}, reason=${result.failureReason}")
        }
        return result
    }

    // ========== 조회 메서드 ==========

    fun getPaymentOrNull(paymentId: Long): Payment? {
        return paymentRepository.findById(paymentId)
    }

    fun getPaymentByOrderId(orderId: Long): Payment? {
        return paymentRepository.findByOrderId(orderId).firstOrNull()
    }

    fun getPaymentsByUser(userId: Long): List<Payment> {
        return paymentRepository.findByUserId(userId)
    }

    fun getPaymentByNumber(paymentNumber: String): Payment? {
        return paymentRepository.findByPaymentNumber(paymentNumber)
    }
}
