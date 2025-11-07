package io.hhplus.ecommerce.payment.controller

import io.hhplus.ecommerce.payment.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.usecase.GetPaymentQueryUseCase
import io.hhplus.ecommerce.payment.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * PaymentController 단위 테스트
 *
 * 책임: 결제 관련 HTTP 요청 처리 검증
 * - REST API 엔드포인트의 요청/응답 처리 검증
 * - UseCase 계층과의 올바른 상호작용 검증
 * - 요청 데이터 변환 및 응답 형식 검증
 *
 * 검증 목표:
 * 1. 각 엔드포인트가 적절한 UseCase를 호출하는가?
 * 2. 요청 파라미터와 Body가 올바르게 UseCase에 전달되는가?
 * 3. UseCase 결과가 적절한 ApiResponse로 변환되는가?
 * 4. HTTP 메서드와 경로 매핑이 올바른가?
 */
class PaymentControllerTest : DescribeSpec({
    val mockProcessPaymentUseCase = mockk<ProcessPaymentUseCase>()
    val mockGetPaymentQueryUseCase = mockk<GetPaymentQueryUseCase>()

    val sut = PaymentController(
        processPaymentUseCase = mockProcessPaymentUseCase,
        getPaymentQueryUseCase = mockGetPaymentQueryUseCase
    )

    beforeEach {
        clearMocks(mockProcessPaymentUseCase, mockGetPaymentQueryUseCase)
    }

    describe("processPayment") {
        context("POST /api/v1/payments/process 요청") {
            it("ProcessPaymentUseCase를 호출하고 ApiResponse로 반환") {
                val request = ProcessPaymentRequest(
                    userId = 1L,
                    orderId = 1L,
                    amount = 10000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val mockPayment = mockk<Payment>()

                every { mockProcessPaymentUseCase.execute(request) } returns mockPayment

                val result = sut.processPayment(request)

                result shouldBe ApiResponse.success(mockPayment)
                verify(exactly = 1) { mockProcessPaymentUseCase.execute(request) }
            }
        }

        context("다른 결제 방법으로 요청") {
            it("모든 결제 방법이 정확히 UseCase에 전달") {
                val request = ProcessPaymentRequest(
                    userId = 2L,
                    orderId = 5L,
                    amount = 50000L,
                    paymentMethod = PaymentMethod.CARD
                )
                val mockPayment = mockk<Payment>()

                every { mockProcessPaymentUseCase.execute(request) } returns mockPayment

                val result = sut.processPayment(request)

                result shouldBe ApiResponse.success(mockPayment)
                verify(exactly = 1) { mockProcessPaymentUseCase.execute(request) }
            }
        }

        context("다양한 금액으로 결제 요청") {
            it("모든 요청이 정확히 UseCase에 전달되는지 확인") {
                val testCases = listOf(
                    Triple(10000L, PaymentMethod.BALANCE, 1L),
                    Triple(50000L, PaymentMethod.CARD, 2L),
                    Triple(100000L, PaymentMethod.BALANCE, 3L)
                )

                testCases.forEachIndexed { index, (amount, paymentMethod, orderId) ->
                    val request = ProcessPaymentRequest(
                        userId = 1L,
                        orderId = orderId,
                        amount = amount,
                        paymentMethod = paymentMethod
                    )
                    val mockPayment = mockk<Payment>()

                    every { mockProcessPaymentUseCase.execute(request) } returns mockPayment

                    val result = sut.processPayment(request)

                    result shouldBe ApiResponse.success(mockPayment)
                    verify(exactly = 1) { mockProcessPaymentUseCase.execute(request) }
                    clearMocks(mockProcessPaymentUseCase)
                }
            }
        }

        context("완전한 요청 객체 전달") {
            it("ProcessPaymentRequest 객체가 그대로 UseCase에 전달") {
                val originalRequest = ProcessPaymentRequest(
                    userId = 42L,
                    orderId = 24L,
                    amount = 84000L,
                    paymentMethod = PaymentMethod.BALANCE
                )
                val mockPayment = mockk<Payment>()

                every { mockProcessPaymentUseCase.execute(originalRequest) } returns mockPayment

                val result = sut.processPayment(originalRequest)

                result shouldBe ApiResponse.success(mockPayment)
                verify(exactly = 1) { mockProcessPaymentUseCase.execute(originalRequest) }
            }
        }
    }

    describe("getPayment") {
        context("GET /api/v1/payments/{paymentId} 요청") {
            it("GetPaymentQueryUseCase를 호출하고 ApiResponse로 반환") {
                val paymentId = 1L
                val mockPayment = mockk<Payment>()

                every { mockGetPaymentQueryUseCase.getPayment(paymentId) } returns mockPayment

                val result = sut.getPayment(paymentId)

                result shouldBe ApiResponse.success(mockPayment)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getPayment(paymentId) }
            }
        }

        context("존재하지 않는 결제 조회") {
            it("null을 ApiResponse로 감싸서 반환") {
                val paymentId = 999L

                every { mockGetPaymentQueryUseCase.getPayment(paymentId) } returns null

                val result = sut.getPayment(paymentId)

                result shouldBe ApiResponse.success(null)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getPayment(paymentId) }
            }
        }

        context("다양한 결제 ID로 조회") {
            it("요청된 paymentId를 정확히 UseCase에 전달") {
                val paymentIds = listOf(1L, 100L, 999L, 1234567L)

                paymentIds.forEach { paymentId ->
                    val mockPayment = mockk<Payment>()
                    every { mockGetPaymentQueryUseCase.getPayment(paymentId) } returns mockPayment

                    val result = sut.getPayment(paymentId)

                    result shouldBe ApiResponse.success(mockPayment)
                    verify(exactly = 1) { mockGetPaymentQueryUseCase.getPayment(paymentId) }
                    clearMocks(mockGetPaymentQueryUseCase)
                }
            }
        }
    }

    describe("getUserPayments") {
        context("GET /api/v1/payments/users/{userId} 요청") {
            it("GetPaymentQueryUseCase를 호출하고 결제 목록을 ApiResponse로 반환") {
                val userId = 1L
                val mockPayments = listOf(mockk<Payment>(), mockk<Payment>())

                every { mockGetPaymentQueryUseCase.getUserPayments(userId) } returns mockPayments

                val result = sut.getUserPayments(userId)

                result shouldBe ApiResponse.success(mockPayments)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getUserPayments(userId) }
            }
        }

        context("결제 내역이 없는 사용자") {
            it("빈 리스트를 ApiResponse로 감싸서 반환") {
                val userId = 999L
                val emptyPayments = emptyList<Payment>()

                every { mockGetPaymentQueryUseCase.getUserPayments(userId) } returns emptyPayments

                val result = sut.getUserPayments(userId)

                result shouldBe ApiResponse.success(emptyPayments)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getUserPayments(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("요청된 userId를 정확히 UseCase에 전달") {
                val userIds = listOf(1L, 100L, 999L, 1234567L)

                userIds.forEach { userId ->
                    val mockPayments = listOf(mockk<Payment>())
                    every { mockGetPaymentQueryUseCase.getUserPayments(userId) } returns mockPayments

                    val result = sut.getUserPayments(userId)

                    result shouldBe ApiResponse.success(mockPayments)
                    verify(exactly = 1) { mockGetPaymentQueryUseCase.getUserPayments(userId) }
                    clearMocks(mockGetPaymentQueryUseCase)
                }
            }
        }

        context("대량 결제 내역 조회") {
            it("많은 결제 내역도 ApiResponse로 정확히 반환") {
                val userId = 1L
                val manyPayments = (1..100).map { mockk<Payment>() }

                every { mockGetPaymentQueryUseCase.getUserPayments(userId) } returns manyPayments

                val result = sut.getUserPayments(userId)

                result shouldBe ApiResponse.success(manyPayments)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getUserPayments(userId) }
            }
        }
    }

    describe("API 경로 및 메서드 검증") {
        context("모든 엔드포인트") {
            it("적절한 UseCase만 호출하고 다른 UseCase는 호출하지 않음") {
                // processPayment 테스트
                val processRequest = ProcessPaymentRequest(1L, 1L, 10000L, PaymentMethod.BALANCE)
                every { mockProcessPaymentUseCase.execute(processRequest) } returns mockk()

                sut.processPayment(processRequest)
                verify(exactly = 1) { mockProcessPaymentUseCase.execute(processRequest) }
                verify(exactly = 0) { mockGetPaymentQueryUseCase.getPayment(any()) }
                verify(exactly = 0) { mockGetPaymentQueryUseCase.getUserPayments(any()) }

                clearMocks(mockProcessPaymentUseCase, mockGetPaymentQueryUseCase)

                // getPayment 테스트
                every { mockGetPaymentQueryUseCase.getPayment(1L) } returns mockk()
                sut.getPayment(1L)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getPayment(1L) }
                verify(exactly = 0) { mockProcessPaymentUseCase.execute(any()) }
                verify(exactly = 0) { mockGetPaymentQueryUseCase.getUserPayments(any()) }

                clearMocks(mockProcessPaymentUseCase, mockGetPaymentQueryUseCase)

                // getUserPayments 테스트
                every { mockGetPaymentQueryUseCase.getUserPayments(1L) } returns emptyList()
                sut.getUserPayments(1L)
                verify(exactly = 1) { mockGetPaymentQueryUseCase.getUserPayments(1L) }
                verify(exactly = 0) { mockProcessPaymentUseCase.execute(any()) }
                verify(exactly = 0) { mockGetPaymentQueryUseCase.getPayment(any()) }
            }
        }
    }

    describe("응답 형식 검증") {
        context("모든 엔드포인트의 응답") {
            it("ApiResponse.success로 감싸서 반환") {
                // processPayment 응답 검증
                val processRequest = ProcessPaymentRequest(1L, 1L, 10000L, PaymentMethod.BALANCE)
                val mockPayment = mockk<Payment>()
                every { mockProcessPaymentUseCase.execute(processRequest) } returns mockPayment

                val processResult = sut.processPayment(processRequest)
                processResult shouldBe ApiResponse.success(mockPayment)

                // getPayment 응답 검증
                every { mockGetPaymentQueryUseCase.getPayment(1L) } returns mockPayment
                val getResult = sut.getPayment(1L)
                getResult shouldBe ApiResponse.success(mockPayment)

                // getUserPayments 응답 검증
                val mockPayments = listOf(mockPayment)
                every { mockGetPaymentQueryUseCase.getUserPayments(1L) } returns mockPayments
                val getUserResult = sut.getUserPayments(1L)
                getUserResult shouldBe ApiResponse.success(mockPayments)
            }
        }
    }
})