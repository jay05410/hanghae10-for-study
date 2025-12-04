package io.hhplus.ecommerce.unit.payment.usecase

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.service.PaymentDomainService
import io.hhplus.ecommerce.payment.application.usecase.GetPaymentQueryUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetPaymentQueryUseCase 단위 테스트
 *
 * 책임: 결제 조회 비즈니스 흐름 검증
 * - PaymentDomainService에 조회 위임 검증
 * - 단일 결제 및 사용자별 결제 목록 조회 검증
 */
class GetPaymentQueryUseCaseTest : DescribeSpec({
    val mockPaymentDomainService = mockk<PaymentDomainService>()
    val sut = GetPaymentQueryUseCase(mockPaymentDomainService)

    beforeEach {
        clearMocks(mockPaymentDomainService)
    }

    describe("getPayment") {
        context("정상적인 결제 조회") {
            it("PaymentDomainService에 조회를 위임하고 결과를 반환") {
                val paymentId = 1L
                val expectedPayment = mockk<Payment>()

                every { mockPaymentDomainService.getPaymentOrNull(paymentId) } returns expectedPayment

                val result = sut.getPayment(paymentId)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentDomainService.getPaymentOrNull(paymentId) }
            }
        }

        context("존재하지 않는 결제 조회") {
            it("null을 반환") {
                val paymentId = 999L

                every { mockPaymentDomainService.getPaymentOrNull(paymentId) } returns null

                val result = sut.getPayment(paymentId)

                result shouldBe null
                verify(exactly = 1) { mockPaymentDomainService.getPaymentOrNull(paymentId) }
            }
        }

        context("다양한 결제 ID 조회") {
            it("모든 결제 ID가 정확히 서비스에 전달되는지 확인") {
                val paymentIds = listOf(1L, 100L, 999L, 1234567L)

                paymentIds.forEach { paymentId ->
                    val expectedPayment = mockk<Payment>()
                    every { mockPaymentDomainService.getPaymentOrNull(paymentId) } returns expectedPayment

                    val result = sut.getPayment(paymentId)

                    result shouldBe expectedPayment
                    verify(exactly = 1) { mockPaymentDomainService.getPaymentOrNull(paymentId) }
                    clearMocks(mockPaymentDomainService)
                }
            }
        }
    }

    describe("getUserPayments") {
        context("정상적인 사용자 결제 목록 조회") {
            it("PaymentDomainService에 조회를 위임하고 결과를 반환") {
                val userId = 1L
                val expectedPayments = listOf(mockk<Payment>(), mockk<Payment>())

                every { mockPaymentDomainService.getPaymentsByUser(userId) } returns expectedPayments

                val result = sut.getUserPayments(userId)

                result shouldBe expectedPayments
                verify(exactly = 1) { mockPaymentDomainService.getPaymentsByUser(userId) }
            }
        }

        context("결제 내역이 없는 사용자") {
            it("빈 리스트를 반환") {
                val userId = 999L
                val emptyPayments = emptyList<Payment>()

                every { mockPaymentDomainService.getPaymentsByUser(userId) } returns emptyPayments

                val result = sut.getUserPayments(userId)

                result shouldBe emptyPayments
                verify(exactly = 1) { mockPaymentDomainService.getPaymentsByUser(userId) }
            }
        }

        context("다양한 사용자 ID 조회") {
            it("모든 사용자 ID가 정확히 서비스에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L, 1234567L)

                userIds.forEach { userId ->
                    val expectedPayments = listOf(mockk<Payment>())
                    every { mockPaymentDomainService.getPaymentsByUser(userId) } returns expectedPayments

                    val result = sut.getUserPayments(userId)

                    result shouldBe expectedPayments
                    verify(exactly = 1) { mockPaymentDomainService.getPaymentsByUser(userId) }
                    clearMocks(mockPaymentDomainService)
                }
            }
        }

        context("대량 결제 내역 조회") {
            it("많은 결제 내역도 정확히 반환") {
                val userId = 1L
                val manyPayments = (1..100).map { mockk<Payment>() }

                every { mockPaymentDomainService.getPaymentsByUser(userId) } returns manyPayments

                val result = sut.getUserPayments(userId)

                result shouldBe manyPayments
                verify(exactly = 1) { mockPaymentDomainService.getPaymentsByUser(userId) }
            }
        }
    }

    describe("getPaymentByOrderId") {
        context("주문 ID로 결제 조회") {
            it("PaymentDomainService에 조회를 위임하고 결과를 반환") {
                val orderId = 1L
                val expectedPayment = mockk<Payment>()

                every { mockPaymentDomainService.getPaymentByOrderId(orderId) } returns expectedPayment

                val result = sut.getPaymentByOrderId(orderId)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentDomainService.getPaymentByOrderId(orderId) }
            }
        }

        context("존재하지 않는 주문 조회") {
            it("null을 반환") {
                val orderId = 999L

                every { mockPaymentDomainService.getPaymentByOrderId(orderId) } returns null

                val result = sut.getPaymentByOrderId(orderId)

                result shouldBe null
            }
        }
    }

    describe("getPaymentByNumber") {
        context("결제번호로 결제 조회") {
            it("PaymentDomainService에 조회를 위임하고 결과를 반환") {
                val paymentNumber = "PAY-001"
                val expectedPayment = mockk<Payment>()

                every { mockPaymentDomainService.getPaymentByNumber(paymentNumber) } returns expectedPayment

                val result = sut.getPaymentByNumber(paymentNumber)

                result shouldBe expectedPayment
                verify(exactly = 1) { mockPaymentDomainService.getPaymentByNumber(paymentNumber) }
            }
        }
    }

    describe("메서드 분리 검증") {
        context("각 조회 메서드가 독립적으로 동작") {
            it("각 메서드가 서로 다른 서비스 메서드 호출") {
                val paymentId = 1L
                val userId = 2L
                val expectedPayment = mockk<Payment>()
                val expectedPayments = listOf(mockk<Payment>())

                every { mockPaymentDomainService.getPaymentOrNull(paymentId) } returns expectedPayment
                every { mockPaymentDomainService.getPaymentsByUser(userId) } returns expectedPayments

                // getPayment 호출
                val paymentResult = sut.getPayment(paymentId)
                paymentResult shouldBe expectedPayment

                // getUserPayments 호출
                val paymentsResult = sut.getUserPayments(userId)
                paymentsResult shouldBe expectedPayments

                // 각각 독립적으로 호출되는지 확인
                verify(exactly = 1) { mockPaymentDomainService.getPaymentOrNull(paymentId) }
                verify(exactly = 1) { mockPaymentDomainService.getPaymentsByUser(userId) }

                // 서로의 메서드는 호출되지 않았는지 확인
                verify(exactly = 0) { mockPaymentDomainService.getPaymentOrNull(userId) }
                verify(exactly = 0) { mockPaymentDomainService.getPaymentsByUser(paymentId) }
            }
        }
    }
})
