package io.hhplus.ecommerce.unit.payment.usecase

import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.payment.application.port.out.PaymentExecutorPort
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.service.PaymentDomainService
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.payment.exception.PaymentException
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher

/**
 * ProcessPaymentUseCase 단위 테스트
 *
 * 책임: 결제 처리 오케스트레이션 검증
 * - 결제 흐름 (검증 → 생성 → 실행 → 결과 처리)
 * - PaymentDomainService와 PaymentExecutorPort 협력 검증
 */
class ProcessPaymentUseCaseTest : DescribeSpec({
    val mockPaymentDomainService = mockk<PaymentDomainService>()
    val mockOutboxEventService = mockk<OutboxEventService>()
    val mockEventPublisher = mockk<ApplicationEventPublisher>()
    val mockBalanceExecutor = mockk<PaymentExecutorPort>()

    beforeEach {
        clearMocks(mockPaymentDomainService, mockOutboxEventService, mockEventPublisher, mockBalanceExecutor)
        every { mockBalanceExecutor.supportedMethod() } returns PaymentMethod.BALANCE
        every { mockOutboxEventService.publishEvent(any(), any(), any(), any()) } returns mockk<OutboxEvent>()
    }

    fun createSut(): ProcessPaymentUseCase {
        return ProcessPaymentUseCase(
            paymentDomainService = mockPaymentDomainService,
            outboxEventService = mockOutboxEventService,
            eventPublisher = mockEventPublisher,
            executors = listOf(mockBalanceExecutor)
        )
    }

    fun createTestPayment(
        id: Long = 1L,
        paymentNumber: String = "PAY-001",
        userId: Long = 1L,
        orderId: Long = 1L,
        amount: Long = 10000L,
        status: PaymentStatus = PaymentStatus.PENDING
    ): Payment = Payment(
        id = id,
        paymentNumber = paymentNumber,
        userId = userId,
        orderId = orderId,
        amount = amount,
        paymentMethod = PaymentMethod.BALANCE,
        status = status
    )

    describe("execute") {
        context("정상적인 결제 처리 요청") {
            it("전체 결제 흐름이 순차적으로 실행") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)
                val processingPayment = createTestPayment(status = PaymentStatus.PROCESSING)
                val completedPayment = createTestPayment(status = PaymentStatus.COMPLETED)

                every { mockPaymentDomainService.validateNoDuplicatePayment(1L) } just runs
                every { mockPaymentDomainService.createPayment(1L, 1L, 10000L, PaymentMethod.BALANCE) } returns pendingPayment
                every { mockBalanceExecutor.canExecute(any()) } returns true
                every { mockPaymentDomainService.markAsProcessing(pendingPayment) } returns processingPayment
                every { mockBalanceExecutor.execute(any()) } returns PaymentResult.success()
                every { mockPaymentDomainService.handlePaymentResult(processingPayment, any()) } returns completedPayment

                val sut = createSut()
                val result = sut.execute(request)

                result shouldBe completedPayment
                verifyOrder {
                    mockPaymentDomainService.validateNoDuplicatePayment(1L)
                    mockPaymentDomainService.createPayment(1L, 1L, 10000L, PaymentMethod.BALANCE)
                    mockBalanceExecutor.canExecute(any())
                    mockPaymentDomainService.markAsProcessing(pendingPayment)
                    mockBalanceExecutor.execute(any())
                    mockPaymentDomainService.handlePaymentResult(processingPayment, any())
                }
            }
        }

        context("중복 결제 시도") {
            it("중복 검증 단계에서 예외 발생") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.BALANCE
                )

                every { mockPaymentDomainService.validateNoDuplicatePayment(1L) } throws
                        PaymentException.DuplicatePayment("이미 결제됨")

                val sut = createSut()

                shouldThrow<PaymentException.DuplicatePayment> {
                    sut.execute(request)
                }

                verify(exactly = 0) { mockPaymentDomainService.createPayment(any(), any(), any(), any()) }
            }
        }

        context("결제 가능 조건 미충족") {
            it("실패 처리 후 예외 발생") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)

                every { mockPaymentDomainService.validateNoDuplicatePayment(1L) } just runs
                every { mockPaymentDomainService.createPayment(1L, 1L, 10000L, PaymentMethod.BALANCE) } returns pendingPayment
                every { mockBalanceExecutor.canExecute(any()) } returns false
                every { mockPaymentDomainService.markAsFailed(pendingPayment, any()) } just runs

                val sut = createSut()

                shouldThrow<PaymentException.PaymentProcessingError> {
                    sut.execute(request)
                }

                verify(exactly = 1) { mockPaymentDomainService.markAsFailed(pendingPayment, any()) }
                verify(exactly = 0) { mockBalanceExecutor.execute(any()) }
            }
        }

        context("지원하지 않는 결제 수단") {
            it("UnsupportedPaymentMethod 예외 발생") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.CARD
                )
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)

                every { mockPaymentDomainService.validateNoDuplicatePayment(1L) } just runs
                every { mockPaymentDomainService.createPayment(1L, 1L, 10000L, PaymentMethod.CARD) } returns pendingPayment

                val sut = createSut()

                shouldThrow<PaymentException.UnsupportedPaymentMethod> {
                    sut.execute(request)
                }
            }
        }

        context("Executor 실행 실패") {
            it("결과 처리에서 예외 발생") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)
                val processingPayment = createTestPayment(status = PaymentStatus.PROCESSING)

                every { mockPaymentDomainService.validateNoDuplicatePayment(1L) } just runs
                every { mockPaymentDomainService.createPayment(1L, 1L, 10000L, PaymentMethod.BALANCE) } returns pendingPayment
                every { mockBalanceExecutor.canExecute(any()) } returns true
                every { mockPaymentDomainService.markAsProcessing(pendingPayment) } returns processingPayment
                every { mockBalanceExecutor.execute(any()) } returns PaymentResult.failure("잔액 부족")
                every { mockPaymentDomainService.handlePaymentResult(processingPayment, any()) } throws
                        PaymentException.PaymentProcessingError("결제 실패")

                val sut = createSut()

                shouldThrow<PaymentException.PaymentProcessingError> {
                    sut.execute(request)
                }
            }
        }
    }
})
