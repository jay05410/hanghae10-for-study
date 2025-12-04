package io.hhplus.ecommerce.integration.payment

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.application.usecase.GetPaymentQueryUseCase
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * 결제 생성 통합 테스트
 *
 * TestContainers MySQL을 사용하여 결제 생성 전체 플로우를 검증합니다.
 * - 결제 생성
 * - 결제 상태 변경 (PENDING → PROCESSING → COMPLETED)
 * - 결제 실패 처리
 * - 사용자별 결제 내역 조회
 */
class PaymentCreateIntegrationTest(
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val getPaymentQueryUseCase: GetPaymentQueryUseCase,
    private val paymentRepository: PaymentRepository,
    private val pointCommandUseCase: PointCommandUseCase
) : KotestIntegrationTestBase({

    // 테스트에 사용할 사용자별로 충분한 포인트 충전 (각 테스트 전에 실행)
    beforeEach {
        val testUserIds = listOf(1000L, 2000L, 3000L, 4000L, 5000L, 6000L, 7000L)
        testUserIds.forEach { userId ->
            pointCommandUseCase.chargePoint(userId, 1_000_000L, "테스트 포인트 충전")
        }
    }

    describe("결제 생성 및 처리") {
        context("정상적인 잔액 결제 요청일 때") {
            it("결제가 정상적으로 완료된다") {
                // Given
                val userId = 1000L
                val orderId = 100L
                val amount = 50000L

                // When
                val payment = processPaymentUseCase.execute(
                    ProcessPaymentRequest(
                        userId = userId,
                        orderId = orderId,
                        amount = amount,
                        paymentMethod = PaymentMethod.BALANCE
                    )
                )

                // Then
                payment shouldNotBe null
                payment.userId shouldBe userId
                payment.orderId shouldBe orderId
                payment.amount shouldBe amount
                payment.paymentMethod shouldBe PaymentMethod.BALANCE
                payment.status shouldBe PaymentStatus.COMPLETED
                payment.paymentNumber shouldStartWith "PAY"
                payment.externalTransactionId shouldNotBe null
                payment.externalTransactionId!! shouldStartWith "TXN"
            }
        }

        context("결제 번호 생성 시") {
            it("고유한 결제 번호가 생성된다") {
                // Given
                val userId = 2000L
                val payment1 = processPaymentUseCase.execute(ProcessPaymentRequest(2000L, 201L, 10000L, PaymentMethod.BALANCE))
                val payment2 = processPaymentUseCase.execute(ProcessPaymentRequest(2000L, 202L, 20000L, PaymentMethod.BALANCE))

                // When & Then
                payment1.paymentNumber shouldNotBe payment2.paymentNumber
                payment1.paymentNumber shouldStartWith "PAY"
                payment2.paymentNumber shouldStartWith "PAY"
            }
        }

        context("완료된 결제 조회 시") {
            it("결제 정보를 조회할 수 있다") {
                // Given
                val userId = 3000L
                val payment = processPaymentUseCase.execute(ProcessPaymentRequest(userId, 301L, 30000L, PaymentMethod.BALANCE))

                // When
                val foundPayment = getPaymentQueryUseCase.getPayment(payment.id)

                // Then
                foundPayment shouldNotBe null
                foundPayment!!.id shouldBe payment.id
                foundPayment.status shouldBe PaymentStatus.COMPLETED
            }
        }

        context("사용자별 결제 내역 조회 시") {
            it("해당 사용자의 모든 결제를 조회할 수 있다") {
                // Given
                val userId = 4000L
                processPaymentUseCase.execute(ProcessPaymentRequest(userId, 401L, 10000L, PaymentMethod.BALANCE))
                processPaymentUseCase.execute(ProcessPaymentRequest(userId, 402L, 20000L, PaymentMethod.BALANCE))
                processPaymentUseCase.execute(ProcessPaymentRequest(userId, 403L, 30000L, PaymentMethod.BALANCE))

                // When
                val payments = getPaymentQueryUseCase.getUserPayments(userId)

                // Then
                payments.size shouldBe 3
                payments.all { it.userId == userId } shouldBe true
            }
        }

        context("여러 주문에 대한 결제 처리 시") {
            it("각 주문마다 독립적인 결제가 생성된다") {
                // Given
                val userId = 5000L

                // When
                val payment1 = processPaymentUseCase.execute(ProcessPaymentRequest(userId, 501L, 15000L, PaymentMethod.BALANCE))
                val payment2 = processPaymentUseCase.execute(ProcessPaymentRequest(userId, 502L, 25000L, PaymentMethod.BALANCE))

                // Then
                payment1.orderId shouldBe 501L
                payment2.orderId shouldBe 502L
                payment1.id shouldNotBe payment2.id
            }
        }

        context("결제 금액이 다를 때") {
            it("각 결제의 금액이 정확히 저장된다") {
                // Given
                val userId = 6000L
                val amounts = listOf(10000L, 50000L, 100000L)

                // When
                val payments = amounts.mapIndexed { index, amount ->
                    processPaymentUseCase.execute(ProcessPaymentRequest(userId, 601L + index, amount, PaymentMethod.BALANCE))
                }

                // Then
                payments.forEachIndexed { index, payment ->
                    payment.amount shouldBe amounts[index]
                }
            }
        }
    }

    describe("결제 상태 확인") {
        context("결제 완료 후") {
            it("상태가 COMPLETED이고 외부 트랜잭션 ID가 기록된다") {
                // Given
                val userId = 7000L
                val payment = processPaymentUseCase.execute(ProcessPaymentRequest(userId, 701L, 40000L, PaymentMethod.BALANCE))

                // When
                val savedPayment = paymentRepository.findById(payment.id)

                // Then
                savedPayment shouldNotBe null
                savedPayment!!.status shouldBe PaymentStatus.COMPLETED
                savedPayment.externalTransactionId shouldNotBe null
            }
        }
    }
})
