package io.hhplus.ecommerce.payment.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.payment.application.port.out.PaymentExecutorPort
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.model.RefundResult
import io.hhplus.ecommerce.payment.domain.service.PaymentDomainService
import io.hhplus.ecommerce.payment.exception.PaymentException
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 환불 처리 유스케이스 - Application Layer
 *
 * 역할:
 * - 환불 처리 비즈니스 흐름 오케스트레이션
 * - 분산락을 통한 중복 환불 방지
 *
 * 책임:
 * - 트랜잭션 경계 관리
 * - 환불 흐름 조정 (조회 → 검증 → 실행 → 결과 처리)
 * - PaymentDomainService 협력 (도메인 로직)
 * - PaymentExecutorPort 협력 (인프라 실행)
 *
 * 동시성 제어:
 * - paymentId 기반 분산락으로 중복 환불 방지
 */
@Component
class RefundPaymentUseCase(
    private val paymentDomainService: PaymentDomainService,
    executors: List<PaymentExecutorPort>
) {
    private val logger = KotlinLogging.logger {}

    private val executorMap: Map<PaymentMethod, PaymentExecutorPort> =
        executors.associateBy { it.supportedMethod() }

    /**
     * 환불 처리 실행
     *
     * 처리 흐름:
     * 1. 결제 조회 (도메인 서비스)
     * 2. 환불 가능 상태 검증 (도메인 서비스)
     * 3. Executor 선택
     * 4. 환불 컨텍스트 생성
     * 5. 환불 실행 (Executor)
     * 6. 결과 처리 (도메인 서비스)
     *
     * @param paymentId 환불할 결제 ID
     * @param reason 환불 사유
     * @return 환불 결과
     * @throws PaymentException 환불 실패 시
     */
    @DistributedLock(
        key = DistributedLockKeys.Payment.REFUND,
        waitTime = 5L,
        leaseTime = 30L
    )
    @DistributedTransaction
    fun execute(paymentId: Long, reason: String? = null): RefundResult {
        logger.info("환불 처리 시작: paymentId=$paymentId, reason=$reason")

        // 1. 결제 조회
        val payment = paymentDomainService.getPayment(paymentId)

        // 2. 환불 가능 상태 검증
        paymentDomainService.validateRefundable(payment)

        // 3. Executor 선택
        val executor = getExecutor(payment.paymentMethod)

        // 4. 환불 컨텍스트 생성
        val context = PaymentContext.forRefund(
            userId = payment.userId,
            orderId = payment.orderId,
            amount = payment.amount,
            paymentMethod = payment.paymentMethod,
            paymentId = payment.id,
            externalTransactionId = payment.externalTransactionId,
            description = reason ?: "주문 취소 환불"
        )

        // 5. 환불 실행
        val result = executor.refund(context)

        // 6. 결과 처리
        return paymentDomainService.handleRefundResult(payment, result)
    }

    private fun getExecutor(paymentMethod: PaymentMethod): PaymentExecutorPort {
        return executorMap[paymentMethod]
            ?: throw PaymentException.UnsupportedPaymentMethod(paymentMethod.name)
    }
}
