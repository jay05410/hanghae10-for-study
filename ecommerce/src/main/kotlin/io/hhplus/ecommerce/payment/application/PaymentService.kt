package io.hhplus.ecommerce.payment.application

import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.payment.exception.PaymentException
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
 * - 결제 상태 및 이력 관리
 *
 * 책임:
 * - 결제 요청 처리 및 결과 관리
 * - 결제 성공/실패 상태 처리
 * - 사용자별 결제 내역 조회
 *
 * 주의:
 * - 동시성 제어는 UseCase 레벨에서 처리
 * - Service는 순수한 비즈니스 로직만 담당
 */
@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val snowflakeGenerator: SnowflakeGenerator
) {

    /**
     * 결제 처리 - 순수한 비즈니스 로직
     *
     * @param userId 사용자 ID
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @param paymentMethod 결제 수단 (기본값: BALANCE)
     * @return 처리 완료된 결제 엔티티
     * @throws PaymentException.DuplicatePayment 중복 결제 시
     * @throws PaymentException.UnsupportedPaymentMethod 지원하지 않는 결제 수단
     */
    @Transactional
    fun processPayment(userId: Long, orderId: Long, amount: Long, paymentMethod: PaymentMethod = PaymentMethod.BALANCE): Payment {
        // 중복 결제 검증 (UseCase에서 분산락 적용으로 이중 보장)
        val existingPayment = paymentRepository.findByOrderId(orderId).firstOrNull()
        if (existingPayment != null) {
            throw PaymentException.DuplicatePayment("주문 ID ${orderId}는 이미 결제 처리되었습니다. 기존 결제 ID: ${existingPayment.id}")
        }

        try {
            // 결제 엔티티 생성
            val paymentNumber = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.PAYMENT)
            val payment = Payment.create(
                paymentNumber = paymentNumber,
                userId = userId,
                orderId = orderId,
                amount = amount,
                paymentMethod = paymentMethod
            )

            val savedPayment = paymentRepository.save(payment)

            when (paymentMethod) {
                PaymentMethod.BALANCE -> {
                    // 잔액 결제는 별도의 서비스에서 처리
                    // 여기서는 결제 엔티티만 관리
                }
                else -> throw PaymentException.UnsupportedPaymentMethod(paymentMethod.name)
            }

            // 결제 처리 중 상태로 전환 (PENDING -> PROCESSING)
            savedPayment.process()
            paymentRepository.save(savedPayment)

            // 결제 완료 처리 (PROCESSING -> COMPLETED)
            savedPayment.complete(
                externalTxId = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.TRANSACTION)
            )

            return paymentRepository.save(savedPayment)

        } catch (e: org.springframework.dao.DataIntegrityViolationException) {
            // 데이터베이스 제약 조건 위반 시 (다른 트랜잭션에서 같은 orderId로 결제가 이미 생성된 경우)
            val existingPaymentAfterConflict = paymentRepository.findByOrderId(orderId).firstOrNull()
            if (existingPaymentAfterConflict != null) {
                throw PaymentException.DuplicatePayment("주문 ID ${orderId}는 이미 결제 처리되었습니다. 기존 결제 ID: ${existingPaymentAfterConflict.id}")
            }
            throw e
        } catch (e: org.springframework.dao.CannotAcquireLockException) {
            // 락 획득 실패 시에도 중복 결제 예외로 변환
            val existingPaymentAfterLockFailure = paymentRepository.findByOrderId(orderId).firstOrNull()
            if (existingPaymentAfterLockFailure != null) {
                throw PaymentException.DuplicatePayment("주문 ID ${orderId}는 이미 결제 처리되었습니다. 기존 결제 ID: ${existingPaymentAfterLockFailure.id}")
            }
            throw PaymentException.DuplicatePayment("주문 ID ${orderId}에 대한 동시 결제 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.")
        } catch (e: Exception) {
            // 결제 실패 처리
            handlePaymentFailure(orderId, e.message ?: "결제 처리 중 오류가 발생했습니다")
            throw e
        }
    }

    /**
     * 결제 실패 처리 - 저장된 결제 엔티티가 있을 경우 상태 업데이트
     */
    private fun handlePaymentFailure(orderId: Long, reason: String) {
        try {
            val existingPayment = paymentRepository.findByOrderId(orderId).firstOrNull()
            existingPayment?.let { payment ->
                payment.fail(reason)
                paymentRepository.save(payment)
            }
        } catch (ignored: Exception) {
            // 실패 상태 업데이트 중 오류는 무시 (이미 실패한 트랜잭션이므로)
        }
    }

    fun getPayment(paymentId: Long): Payment? {
        return paymentRepository.findById(paymentId)
    }

    /**
     * 결제를 조회한다
     *
     * @param paymentId 조회할 결제 ID
     * @return Payment
     *
     * 참고: PaymentHistory는 별도 Repository로 조회해야 합니다 (OneToMany 관계 제거됨)
     */
    @Transactional(readOnly = true)
    fun getPaymentWithHistories(paymentId: Long): Payment? {
        return paymentRepository.findById(paymentId)
    }

    fun getPaymentsByUser(userId: Long): List<Payment> {
        return paymentRepository.findByUserId(userId)
    }

    /**
     * 결제번호로 결제를 조회한다
     *
     * @param paymentNumber 조회할 결제번호
     * @return Payment
     *
     * 참고: PaymentHistory는 별도 Repository로 조회해야 합니다 (OneToMany 관계 제거됨)
     */
    @Transactional(readOnly = true)
    fun getPaymentWithHistoriesByNumber(paymentNumber: String): Payment? {
        return paymentRepository.findByPaymentNumber(paymentNumber)
    }

}