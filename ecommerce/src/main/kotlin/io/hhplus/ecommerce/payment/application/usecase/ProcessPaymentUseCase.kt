package io.hhplus.ecommerce.payment.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.payment.application.port.out.PaymentExecutorPort
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.service.PaymentDomainService
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.payment.exception.PaymentException
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 결제 처리 유스케이스 - Application Layer
 *
 * 역할:
 * - 결제 처리 비즈니스 흐름 오케스트레이션
 * - 분산락을 통한 중복 결제 방지
 * - 결제 수단별 Executor 선택 (전략 패턴)
 *
 * 책임:
 * - 트랜잭션 경계 관리
 * - 결제 흐름 조정 (검증 → 생성 → 실행 → 결과 처리)
 * - PaymentDomainService 협력 (도메인 로직)
 * - PaymentExecutorPort 협력 (인프라 실행)
 *
 * 이벤트 발행:
 * - 결제 성공 시 PaymentCompleted 이벤트 발행
 * - 후속 처리(주문 확정, 재고 확정 등)는 이벤트 핸들러에서 처리
 *
 * 동시성 제어:
 * - orderId 기반 분산락으로 중복 결제 방지
 * - 분산 트랜잭션으로 데이터 정합성 보장
 */
@Component
class ProcessPaymentUseCase(
    private val paymentDomainService: PaymentDomainService,
    private val outboxEventService: OutboxEventService,
    executors: List<PaymentExecutorPort>
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    // 결제 수단별 Executor 맵 구성
    private val executorMap: Map<PaymentMethod, PaymentExecutorPort> =
        executors.associateBy { it.supportedMethod() }

    init {
        logger.info("ProcessPaymentUseCase 초기화: 등록된 Executor = ${executorMap.keys}")
    }

    /**
     * 결제 처리 실행
     *
     * 처리 흐름:
     * 1. 중복 결제 검증 (도메인 서비스)
     * 2. 결제 엔티티 생성 - PENDING (도메인 서비스)
     * 3. 결제 수단별 Executor 선택
     * 4. 결제 가능 여부 검증 (Executor)
     * 5. 상태 전환 PENDING → PROCESSING (도메인 서비스)
     * 6. 결제 실행 (Executor)
     * 7. 결과 처리 → COMPLETED/FAILED (도메인 서비스)
     *
     * @param request 결제 처리 요청
     * @return 처리 완료된 결제 정보
     * @throws PaymentException 결제 실패 시
     */
    @DistributedLock(
        key = "${DistributedLockKeys.Payment.DUPLICATE_PREVENTION}:#{#request.orderId}",
        waitTime = 5L,
        leaseTime = 30L
    )
    @DistributedTransaction
    fun execute(request: ProcessPaymentRequest): Payment {
        logger.info("결제 처리 시작: userId=${request.userId}, orderId=${request.orderId}, amount=${request.amount}, method=${request.paymentMethod}")

        // 1. 중복 결제 검증
        paymentDomainService.validateNoDuplicatePayment(request.orderId)

        // 2. 결제 엔티티 생성 (PENDING)
        var payment = paymentDomainService.createPayment(
            userId = request.userId,
            orderId = request.orderId,
            amount = request.amount,
            paymentMethod = request.paymentMethod
        )

        // 3. Executor 선택
        val executor = getExecutor(request.paymentMethod)

        // 4. 결제 컨텍스트 생성
        val context = PaymentContext.forPayment(
            userId = request.userId,
            orderId = request.orderId,
            amount = request.amount,
            paymentMethod = request.paymentMethod
        )

        // 5. 결제 가능 여부 검증
        if (!executor.canExecute(context)) {
            paymentDomainService.markAsFailed(payment, "결제 가능 조건을 충족하지 않습니다 (잔액 부족 등)")
            throw PaymentException.PaymentProcessingError("결제 가능 조건을 충족하지 않습니다")
        }

        // 6. 상태 전환: PENDING → PROCESSING
        payment = paymentDomainService.markAsProcessing(payment)

        // 7. 결제 실행
        val result = executor.execute(context)

        // 8. 결과 처리 → COMPLETED/FAILED
        val completedPayment = paymentDomainService.handlePaymentResult(payment, result)

        // 9. 결제 성공 시 PaymentCompleted 이벤트 발행
        if (completedPayment.status == PaymentStatus.COMPLETED) {
            publishPaymentCompletedEvent(request, completedPayment)
            logger.info("결제 성공, PaymentCompleted 이벤트 발행: paymentId=${completedPayment.id}")
        }

        return completedPayment
    }

    private fun publishPaymentCompletedEvent(request: ProcessPaymentRequest, payment: Payment) {
        val payload = PaymentCompletedPayload(
            orderId = request.orderId,
            userId = request.userId,
            paymentId = payment.id,
            amount = request.amount
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.PAYMENT_COMPLETED,
            aggregateType = EventRegistry.AggregateTypes.PAYMENT,
            aggregateId = payment.id.toString(),
            payload = json.encodeToString(PaymentCompletedPayload.serializer(), payload)
        )
    }

    private fun getExecutor(paymentMethod: PaymentMethod): PaymentExecutorPort {
        return executorMap[paymentMethod]
            ?: throw PaymentException.UnsupportedPaymentMethod(paymentMethod.name)
    }
}
