package io.hhplus.ecommerce.integration.payment

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.payment.exception.PaymentException
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/**
 * 결제 중복 방지 통합 테스트
 *
 * 목적:
 * - 동일한 주문 ID로 동시 결제 시도 시 중복 방지 검증
 * - 분산락을 통한 순차 처리 확인
 */
class PaymentDuplicatePreventionTest(
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val chargePointUseCase: ChargePointUseCase
) : KotestIntegrationTestBase({

    // 테스트에 사용할 사용자별로 충분한 포인트 충전 (각 테스트 전에 실행)
    beforeEach {
        val testUserIds = listOf(50001L, 50002L, 50003L)
        testUserIds.forEach { userId ->
            chargePointUseCase.execute(userId, 1_000_000L, "테스트 포인트 충전")
        }
    }

    describe("결제 중복 방지") {
        context("동일한 주문 ID로 동시 결제를 시도할 때") {
            it("첫 번째 결제만 성공하고 나머지는 중복 예외가 발생해야 한다") {
                // Given
                val userId = 50001L
                val orderId = 50001L
                val amount = 10000L
                val threadCount = 3 // 줄여서 더 안정적으로 테스트

                // When: 동시에 동일한 주문 ID로 결제 시도
                runBlocking {
                    val results = (1..threadCount).map { index ->
                        async {
                            runCatching {
                                processPaymentUseCase.execute(
                                    ProcessPaymentRequest(
                                        userId = userId,
                                        orderId = orderId,
                                        amount = amount,
                                        paymentMethod = PaymentMethod.BALANCE
                                    )
                                )
                            }
                        }
                    }.awaitAll()

                    // Then: 하나만 성공하고 나머지는 실패
                    val successes = results.filter { it.isSuccess }
                    val failures = results.filter { it.isFailure }

                    // 최소 1개는 성공, 나머지는 실패해야 함 (동시성으로 인해 정확히 1개가 아닐 수도 있음)
                    successes.size shouldBe 1
                    failures.size shouldBe (threadCount - 1)

                    // 실패한 경우들이 적절한 예외인지 확인
                    failures.forEach { result ->
                        val exception = result.exceptionOrNull()
                        val isAcceptableException = when (exception) {
                            is PaymentException.DuplicatePayment -> true
                            is org.springframework.dao.DataIntegrityViolationException -> true
                            is org.springframework.dao.CannotAcquireLockException -> true
                            else -> false
                        }

                        if (!isAcceptableException) {
                            throw AssertionError("Unexpected exception type: ${exception?.javaClass?.simpleName} - ${exception?.message}")
                        }
                    }
                }
            }
        }

        context("서로 다른 주문 ID로 결제를 시도할 때") {
            it("모든 결제가 성공해야 한다") {
                // Given
                val userId = 50002L
                val baseOrderId = 50010L
                val amount = 10000L
                val threadCount = 3

                // When: 서로 다른 주문 ID로 동시 결제
                runBlocking {
                    val results = (1..threadCount).map { index ->
                        async {
                            processPaymentUseCase.execute(
                                ProcessPaymentRequest(
                                    userId = userId,
                                    orderId = baseOrderId + index,
                                    amount = amount,
                                    paymentMethod = PaymentMethod.BALANCE
                                )
                            )
                        }
                    }.awaitAll()

                    // Then: 모든 결제가 성공
                    results.size shouldBe threadCount
                    results.forEach { payment ->
                        payment.userId shouldBe userId
                        payment.amount shouldBe amount
                        payment.status.name shouldBe "COMPLETED"
                    }

                    // 주문 ID가 모두 다른지 확인
                    val orderIds = results.map { it.orderId }.toSet()
                    orderIds.size shouldBe threadCount
                }
            }
        }

        context("이미 결제된 주문에 다시 결제를 시도할 때") {
            it("중복 결제 예외가 발생해야 한다") {
                // Given: 첫 번째 결제 성공
                val userId = 50003L
                val orderId = 50020L
                val amount = 10000L

                val firstPayment = processPaymentUseCase.execute(
                    ProcessPaymentRequest(
                        userId = userId,
                        orderId = orderId,
                        amount = amount,
                        paymentMethod = PaymentMethod.BALANCE
                    )
                )
                firstPayment.status.name shouldBe "COMPLETED"

                // When & Then: 동일한 주문으로 다시 결제 시도
                val exception = shouldThrow<PaymentException.DuplicatePayment> {
                    processPaymentUseCase.execute(
                        ProcessPaymentRequest(
                            userId = userId,
                            orderId = orderId,
                            amount = amount,
                            paymentMethod = PaymentMethod.BALANCE
                        )
                    )
                }

                exception.message shouldContain orderId.toString()
                exception.message shouldContain "이미 결제 처리되었습니다"
                exception.message shouldContain firstPayment.id.toString()
            }
        }
    }
})
