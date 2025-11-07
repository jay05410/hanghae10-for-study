package io.hhplus.ecommerce.payment.application

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
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

    fun createMockPayment(
        id: Long = 1L,
        paymentNumber: String = "PAY-001",
        userId: Long = 1L,
        orderId: Long = 1L,
        amount: Long = 10000L,
        status: PaymentStatus = PaymentStatus.COMPLETED
    ): Payment = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.paymentNumber } returns paymentNumber
        every { this@mockk.userId } returns userId
        every { this@mockk.orderId } returns orderId
        every { this@mockk.amount } returns amount
        every { this@mockk.status } returns status
        every { paymentMethod } returns PaymentMethod.BALANCE
        every { createdAt } returns LocalDateTime.now()
        every { complete(any(), any()) } returns Unit
        every { fail(any(), any()) } returns Unit
    }

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
                val mockPayment = createMockPayment(
                    paymentNumber = paymentNumber,
                    userId = userId,
                    orderId = orderId,
                    amount = amount
                )

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returnsMany listOf(paymentNumber, txId)
                every { mockPaymentRepository.save(any()) } returns mockPayment

                val result = sut.processPayment(userId, orderId, amount)

                result shouldBe mockPayment
                verify(exactly = 2) { mockSnowflakeGenerator.generateNumberWithPrefix(any()) }
                verify(exactly = 2) { mockPaymentRepository.save(any()) }
                verify(exactly = 1) { mockPayment.complete(userId, txId) }
            }
        }
    }

    describe("getPayment") {
        context("존재하는 결제 조회") {
            it("Repository에서 결제를 조회하여 반환") {
                val paymentId = 1L
                val mockPayment = createMockPayment(id = paymentId)

                every { mockPaymentRepository.findById(paymentId) } returns mockPayment

                val result = sut.getPayment(paymentId)

                result shouldBe mockPayment
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
                val mockPayments = listOf(
                    createMockPayment(id = 1L, userId = userId),
                    createMockPayment(id = 2L, userId = userId)
                )

                every { mockPaymentRepository.findByUserId(userId) } returns mockPayments

                val result = sut.getPaymentsByUser(userId)

                result shouldHaveSize 2
                result shouldBe mockPayments
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