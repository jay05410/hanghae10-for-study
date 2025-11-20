package io.hhplus.ecommerce.unit.payment.application

import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.payment.exception.PaymentException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import java.time.LocalDateTime

/**
 * PaymentService 단위 테스트
 *
 * 책임: 결제 서비스의 핵심 비즈니스 로직 검증
 * - 결제 처리 및 상태 관리 기능 검증
 * - Repository와의 상호작용 검증
 */
class PaymentServiceTest : DescribeSpec({
    val mockPaymentRepository = mockk<PaymentRepository>()
    val mockSnowflakeGenerator = mockk<SnowflakeGenerator>()

    val sut = PaymentService(
        paymentRepository = mockPaymentRepository,
        snowflakeGenerator = mockSnowflakeGenerator
    )

    fun createTestPayment(
        id: Long = 1L,
        paymentNumber: String = "PAY-001",
        userId: Long = 1L,
        orderId: Long = 1L,
        amount: Long = 10000L,
        status: PaymentStatus = PaymentStatus.COMPLETED
    ): Payment = Payment(
        id = id,
        paymentNumber = paymentNumber,
        userId = userId,
        orderId = orderId,
        amount = amount,
        paymentMethod = PaymentMethod.BALANCE,
        status = status
    )

    beforeEach {
        clearMocks(mockPaymentRepository, mockSnowflakeGenerator)
    }

    describe("processPayment") {
        context("정상적인 결제 처리") {
            it("결제를 생성하고 완료 처리") {
                val userId = 1L
                val orderId = 1L
                val amount = 10000L
                val paymentNumber = "PAY-001"
                val txId = "TXN-001"
                val pendingPayment = mockk<Payment>(relaxed = true) {
                    every { status } returns PaymentStatus.COMPLETED
                    every { process() } just runs
                    every { complete(any()) } just runs
                }

                mockkObject(Payment.Companion)
                every { Payment.create(any(), any(), any(), any(), any()) } returns pendingPayment

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returnsMany listOf(paymentNumber, txId)
                every { mockPaymentRepository.save(any()) } returns pendingPayment

                val result = sut.processPayment(userId, orderId, amount)

                result.status shouldBe PaymentStatus.COMPLETED
                verify(exactly = 2) { mockSnowflakeGenerator.generateNumberWithPrefix(any()) }
                verify(exactly = 3) { mockPaymentRepository.save(any()) }
            }
        }

        context("지원되지 않는 결제 수단") {
            it("CARD 결제 시 UnsupportedPaymentMethod 예외를 발생") {
                val userId = 1L
                val orderId = 1L
                val amount = 10000L
                val paymentNumber = "PAY-001"
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)
                val failedPayment = pendingPayment.copy(status = PaymentStatus.FAILED)

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returns paymentNumber
                every { mockPaymentRepository.save(any()) } returnsMany listOf(pendingPayment, failedPayment)

                shouldThrow<PaymentException.UnsupportedPaymentMethod> {
                    sut.processPayment(userId, orderId, amount, PaymentMethod.CARD)
                }

                verify(atLeast = 1) { mockPaymentRepository.save(any()) }
            }

            it("BANK_TRANSFER 결제 시 UnsupportedPaymentMethod 예외를 발생") {
                val userId = 1L
                val orderId = 1L
                val amount = 10000L
                val paymentNumber = "PAY-001"
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)
                val failedPayment = pendingPayment.copy(status = PaymentStatus.FAILED)

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returns paymentNumber
                every { mockPaymentRepository.save(any()) } returnsMany listOf(pendingPayment, failedPayment)

                shouldThrow<PaymentException.UnsupportedPaymentMethod> {
                    sut.processPayment(userId, orderId, amount, PaymentMethod.BANK_TRANSFER)
                }

                verify(atLeast = 1) { mockPaymentRepository.save(any()) }
            }
        }

        context("결제 처리 중 예외 발생") {
            it("Repository 저장 실패 시 결제를 실패 처리하고 예외를 재발생") {
                val userId = 1L
                val orderId = 1L
                val amount = 10000L
                val paymentNumber = "PAY-001"
                val txId = "TXN-001"
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)
                val failedPayment = pendingPayment.copy(status = PaymentStatus.FAILED)
                val dbException = RuntimeException("DB 연결 오류")

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returnsMany listOf(paymentNumber, txId)
                every { mockPaymentRepository.save(any()) } returns pendingPayment andThenThrows dbException andThen failedPayment

                shouldThrow<RuntimeException> {
                    sut.processPayment(userId, orderId, amount)
                }.message shouldBe "DB 연결 오류"

                verify(atLeast = 1) { mockPaymentRepository.save(any()) }
            }

            it("예외 메시지가 null인 경우 기본 메시지로 실패 처리") {
                val userId = 1L
                val orderId = 1L
                val amount = 10000L
                val paymentNumber = "PAY-001"
                val txId = "TXN-001"
                val pendingPayment = createTestPayment(status = PaymentStatus.PENDING)
                val failedPayment = pendingPayment.copy(status = PaymentStatus.FAILED)
                val nullMessageException = RuntimeException()

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returnsMany listOf(paymentNumber, txId)
                every { mockPaymentRepository.save(any()) } returns pendingPayment andThenThrows nullMessageException andThen failedPayment

                shouldThrow<RuntimeException> {
                    sut.processPayment(userId, orderId, amount)
                }

                verify(atLeast = 1) { mockPaymentRepository.save(any()) }
            }
        }
    }

    describe("getPayment") {
        context("존재하는 결제 조회") {
            it("Repository에서 결제를 조회하여 반환") {
                val paymentId = 1L
                val payment = createTestPayment(id = paymentId)

                every { mockPaymentRepository.findById(paymentId) } returns payment

                val result = sut.getPayment(paymentId)

                result shouldBe payment
                verify(exactly = 1) { mockPaymentRepository.findById(paymentId) }
            }
        }

        context("존재하지 않는 결제 조회") {
            it("null을 반환") {
                val paymentId = 999L

                every { mockPaymentRepository.findById(paymentId) } returns null

                val result = sut.getPayment(paymentId)

                result shouldBe null
                verify(exactly = 1) { mockPaymentRepository.findById(paymentId) }
            }
        }
    }

    describe("getPaymentsByUser") {
        context("사용자의 결제가 있는 경우") {
            it("사용자의 결제 목록을 반환") {
                val userId = 1L
                val payments = listOf(
                    createTestPayment(id = 1L, userId = userId),
                    createTestPayment(id = 2L, userId = userId)
                )

                every { mockPaymentRepository.findByUserId(userId) } returns payments

                val result = sut.getPaymentsByUser(userId)

                result shouldHaveSize 2
                result shouldBe payments
                verify(exactly = 1) { mockPaymentRepository.findByUserId(userId) }
            }
        }

        context("사용자의 결제가 없는 경우") {
            it("빈 목록을 반환") {
                val userId = 2L

                every { mockPaymentRepository.findByUserId(userId) } returns emptyList()

                val result = sut.getPaymentsByUser(userId)

                result.shouldBeEmpty()
                verify(exactly = 1) { mockPaymentRepository.findByUserId(userId) }
            }
        }
    }
})