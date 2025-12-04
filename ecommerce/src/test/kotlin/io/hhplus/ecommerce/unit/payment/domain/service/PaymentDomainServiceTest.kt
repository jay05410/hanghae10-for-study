package io.hhplus.ecommerce.unit.payment.domain.service

import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.model.RefundResult
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.payment.domain.service.PaymentDomainService
import io.hhplus.ecommerce.payment.exception.PaymentException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * PaymentDomainService 단위 테스트
 *
 * 책임: 결제 도메인 로직 검증
 * - 결제 엔티티 생성/조회
 * - 중복 결제 검증
 * - 상태 전환 처리
 * - 결제/환불 결과 처리
 */
class PaymentDomainServiceTest : DescribeSpec({
    val mockPaymentRepository = mockk<PaymentRepository>()
    val mockSnowflakeGenerator = mockk<SnowflakeGenerator>()

    val sut = PaymentDomainService(
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

    describe("validateNoDuplicatePayment") {
        context("중복 결제가 없을 때") {
            it("예외 없이 통과") {
                val orderId = 1L
                every { mockPaymentRepository.findByOrderId(orderId) } returns emptyList()

                sut.validateNoDuplicatePayment(orderId)

                verify(exactly = 1) { mockPaymentRepository.findByOrderId(orderId) }
            }
        }

        context("이미 결제가 존재할 때") {
            it("DuplicatePayment 예외를 발생") {
                val orderId = 1L
                val existingPayment = createTestPayment(orderId = orderId)
                every { mockPaymentRepository.findByOrderId(orderId) } returns listOf(existingPayment)

                shouldThrow<PaymentException.DuplicatePayment> {
                    sut.validateNoDuplicatePayment(orderId)
                }
            }
        }
    }

    describe("createPayment") {
        context("정상적인 결제 생성 요청") {
            it("PENDING 상태의 결제를 생성하고 저장") {
                val userId = 1L
                val orderId = 1L
                val amount = 10000L
                val paymentNumber = "PAY-001"

                every { mockSnowflakeGenerator.generateNumberWithPrefix(any()) } returns paymentNumber
                every { mockPaymentRepository.save(any()) } answers { firstArg() }

                val result = sut.createPayment(userId, orderId, amount, PaymentMethod.BALANCE)

                result.userId shouldBe userId
                result.orderId shouldBe orderId
                result.amount shouldBe amount
                result.status shouldBe PaymentStatus.PENDING
                result.paymentMethod shouldBe PaymentMethod.BALANCE
                verify(exactly = 1) { mockPaymentRepository.save(any()) }
            }
        }
    }

    describe("markAsProcessing") {
        context("PENDING 상태의 결제") {
            it("PROCESSING 상태로 전환") {
                val payment = createTestPayment(status = PaymentStatus.PENDING)
                every { mockPaymentRepository.save(any()) } answers { firstArg() }

                val result = sut.markAsProcessing(payment)

                result.status shouldBe PaymentStatus.PROCESSING
                verify(exactly = 1) { mockPaymentRepository.save(any()) }
            }
        }
    }

    describe("handlePaymentResult") {
        context("결제 성공 결과") {
            it("COMPLETED 상태로 전환하고 외부 트랜잭션 ID 저장") {
                val payment = createTestPayment(status = PaymentStatus.PROCESSING)
                val result = PaymentResult.success(externalTransactionId = "TXN-123")
                every { mockPaymentRepository.save(any()) } answers { firstArg() }

                val completedPayment = sut.handlePaymentResult(payment, result)

                completedPayment.status shouldBe PaymentStatus.COMPLETED
                completedPayment.externalTransactionId shouldBe "TXN-123"
            }
        }

        context("결제 실패 결과") {
            it("FAILED 상태로 전환하고 PaymentProcessingError 예외 발생") {
                val payment = createTestPayment(status = PaymentStatus.PROCESSING)
                val result = PaymentResult.failure("잔액 부족")
                every { mockPaymentRepository.save(any()) } answers { firstArg() }

                shouldThrow<PaymentException.PaymentProcessingError> {
                    sut.handlePaymentResult(payment, result)
                }
            }
        }
    }

    describe("markAsFailed") {
        context("실패 처리 요청") {
            it("FAILED 상태로 전환하고 실패 사유 저장") {
                val payment = createTestPayment(status = PaymentStatus.PENDING)
                val reason = "잔액 부족"
                every { mockPaymentRepository.save(any()) } answers { firstArg() }

                sut.markAsFailed(payment, reason)

                payment.status shouldBe PaymentStatus.FAILED
                payment.failureReason shouldBe reason
                verify(exactly = 1) { mockPaymentRepository.save(any()) }
            }
        }
    }

    describe("getPayment") {
        context("존재하는 결제 조회") {
            it("결제를 반환") {
                val paymentId = 1L
                val payment = createTestPayment(id = paymentId)
                every { mockPaymentRepository.findById(paymentId) } returns payment

                val result = sut.getPayment(paymentId)

                result shouldBe payment
            }
        }

        context("존재하지 않는 결제 조회") {
            it("PaymentProcessingError 예외 발생") {
                val paymentId = 999L
                every { mockPaymentRepository.findById(paymentId) } returns null

                shouldThrow<PaymentException.PaymentProcessingError> {
                    sut.getPayment(paymentId)
                }
            }
        }
    }

    describe("validateRefundable") {
        context("COMPLETED 상태의 결제") {
            it("예외 없이 통과") {
                val payment = createTestPayment(status = PaymentStatus.COMPLETED)

                sut.validateRefundable(payment)
            }
        }

        context("COMPLETED가 아닌 상태의 결제") {
            it("PaymentProcessingError 예외 발생") {
                val payment = createTestPayment(status = PaymentStatus.PENDING)

                shouldThrow<PaymentException.PaymentProcessingError> {
                    sut.validateRefundable(payment)
                }
            }
        }
    }

    describe("handleRefundResult") {
        context("환불 성공 결과") {
            it("CANCELLED 상태로 전환") {
                val payment = createTestPayment(status = PaymentStatus.COMPLETED)
                val result = RefundResult.success(refundedAmount = 10000L)
                every { mockPaymentRepository.save(any()) } answers { firstArg() }

                val refundResult = sut.handleRefundResult(payment, result)

                refundResult.success shouldBe true
                payment.status shouldBe PaymentStatus.CANCELLED
            }
        }

        context("환불 실패 결과") {
            it("상태 변경 없이 결과 반환") {
                val payment = createTestPayment(status = PaymentStatus.COMPLETED)
                val result = RefundResult.failure("환불 실패")

                val refundResult = sut.handleRefundResult(payment, result)

                refundResult.success shouldBe false
                payment.status shouldBe PaymentStatus.COMPLETED
            }
        }
    }

    describe("조회 메서드") {
        context("getPaymentOrNull") {
            it("존재하면 결제 반환, 없으면 null") {
                val paymentId = 1L
                val payment = createTestPayment(id = paymentId)
                every { mockPaymentRepository.findById(paymentId) } returns payment
                every { mockPaymentRepository.findById(999L) } returns null

                sut.getPaymentOrNull(paymentId) shouldBe payment
                sut.getPaymentOrNull(999L) shouldBe null
            }
        }

        context("getPaymentsByUser") {
            it("사용자의 결제 목록 반환") {
                val userId = 1L
                val payments = listOf(
                    createTestPayment(id = 1L, userId = userId),
                    createTestPayment(id = 2L, userId = userId)
                )
                every { mockPaymentRepository.findByUserId(userId) } returns payments

                val result = sut.getPaymentsByUser(userId)

                result shouldHaveSize 2
            }

            it("결제가 없으면 빈 목록 반환") {
                val userId = 999L
                every { mockPaymentRepository.findByUserId(userId) } returns emptyList()

                val result = sut.getPaymentsByUser(userId)

                result.shouldBeEmpty()
            }
        }

        context("getPaymentByOrderId") {
            it("주문 ID로 결제 조회") {
                val orderId = 1L
                val payment = createTestPayment(orderId = orderId)
                every { mockPaymentRepository.findByOrderId(orderId) } returns listOf(payment)

                val result = sut.getPaymentByOrderId(orderId)

                result shouldBe payment
            }

            it("결제가 없으면 null 반환") {
                val orderId = 999L
                every { mockPaymentRepository.findByOrderId(orderId) } returns emptyList()

                val result = sut.getPaymentByOrderId(orderId)

                result shouldBe null
            }
        }
    }
})
