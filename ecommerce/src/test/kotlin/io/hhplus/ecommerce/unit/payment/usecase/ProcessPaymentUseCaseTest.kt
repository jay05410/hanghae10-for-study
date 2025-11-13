package io.hhplus.ecommerce.unit.payment.usecase

import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ProcessPaymentUseCase 단위 테스트
 *
 * 책임: 결제 처리 비즈니스 흐름 검증
 * - 결제 처리 로직의 서비스 위임 검증
 * - 요청 DTO에서 파라미터 추출 및 전달 검증
 *
 * 검증 목표:
 * 1. PaymentService에 올바른 파라미터가 전달되는가?
 * 2. ProcessPaymentRequest의 모든 필드가 정확히 전달되는가?
 * 3. 서비스 결과가 그대로 반환되는가?
 */
class ProcessPaymentUseCaseTest : DescribeSpec({
    val mockPaymentService = mockk<PaymentService>()
    val sut = ProcessPaymentUseCase(mockPaymentService)

    beforeEach {
        clearMocks(mockPaymentService)
    }

    describe("execute") {
        context("정상적인 결제 처리 요청") {
            it("PaymentService에 처리를 위임하고 결과를 반환") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val expectedPayment = mockk<Payment>()

                every { mockPaymentService.processPayment(1L, 1L, 10000L, PaymentMethod.BALANCE) } returns expectedPayment

                val result = sut.execute(request)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentService.processPayment(1L, 1L, 10000L, PaymentMethod.BALANCE) }
            }
        }

        context("다른 결제 방법으로 요청") {
            it("요청된 결제 방법이 정확히 서비스에 전달") {
                val request = ProcessPaymentRequest(
                    userId = 2L,
                    orderId = 5L,
                    amount = 50000L,
                    paymentMethod = PaymentMethod.CARD
                )
                val expectedPayment = mockk<Payment>()

                every { mockPaymentService.processPayment(2L, 5L, 50000L, PaymentMethod.CARD) } returns expectedPayment

                val result = sut.execute(request)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentService.processPayment(2L, 5L, 50000L, PaymentMethod.CARD) }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 서비스에 전달되는지 확인") {
                data class TestCase(
                    val userId: Long,
                    val orderId: Long,
                    val amount: Long,
                    val paymentMethod: PaymentMethod
                )

                val testCases = listOf(
                    TestCase(1L, 1L, 10000L, PaymentMethod.BALANCE),
                    TestCase(100L, 200L, 75000L, PaymentMethod.CARD),
                    TestCase(999L, 888L, 150000L, PaymentMethod.BALANCE)
                )

                testCases.forEach { testCase ->
                    val request = ProcessPaymentRequest(
                        userId = testCase.userId,
                        orderId = testCase.orderId,
                        amount = testCase.amount,
                        paymentMethod = testCase.paymentMethod
                    )
                    val expectedPayment = mockk<Payment>()

                    every { mockPaymentService.processPayment(testCase.userId, testCase.orderId, testCase.amount, testCase.paymentMethod) } returns expectedPayment

                    val result = sut.execute(request)

                    result shouldBe expectedPayment
                    verify(exactly = 1) { mockPaymentService.processPayment(testCase.userId, testCase.orderId, testCase.amount, testCase.paymentMethod) }
                    clearMocks(mockPaymentService)
                }
            }
        }

        context("큰 금액 결제 처리") {
            it("큰 금액도 정확히 전달되고 처리") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 1_000_000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val expectedPayment = mockk<Payment>()

                every { mockPaymentService.processPayment(1L, 1L, 1_000_000L, PaymentMethod.BALANCE) } returns expectedPayment

                val result = sut.execute(request)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentService.processPayment(1L, 1L, 1_000_000L, PaymentMethod.BALANCE) }
            }
        }

        context("파라미터 순서 검증") {
            it("userId, orderId, amount, paymentMethod 순서로 정확히 전달") {
                val userId = 42L
                val orderId = 24L
                val amount = 84000L
                val paymentMethod = PaymentMethod.BALANCE
                val request = ProcessPaymentRequest(
                    userId = userId,
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = paymentMethod
                )
                val expectedPayment = mockk<Payment>()

                every { mockPaymentService.processPayment(userId, orderId, amount, paymentMethod) } returns expectedPayment

                val result = sut.execute(request)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentService.processPayment(
                    userId,          // 첫 번째 파라미터
                    orderId,         // 두 번째 파라미터
                    amount,          // 세 번째 파라미터
                    paymentMethod    // 네 번째 파라미터
                ) }
            }
        }
    }
})