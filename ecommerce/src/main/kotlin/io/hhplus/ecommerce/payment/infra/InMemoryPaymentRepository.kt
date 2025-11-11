package io.hhplus.ecommerce.payment.infra

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 결제 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 결제 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - PaymentRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryPaymentRepository : PaymentRepository {
    private val payments = ConcurrentHashMap<Long, Payment>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val payment1 = Payment.create(
            paymentNumber = "PAY-001-20241107",
            orderId = 1L,
            userId = 1L,
            amount = 40000L,
            paymentMethod = PaymentMethod.BALANCE,
            createdBy = 1L
        ).let {
            Payment(
                id = idGenerator.getAndIncrement(),
                paymentNumber = it.paymentNumber,
                orderId = it.orderId,
                userId = it.userId,
                amount = it.amount,
                paymentMethod = it.paymentMethod,
                status = PaymentStatus.COMPLETED,
                externalTransactionId = "TXN-BAL-001",
                failureReason = null
            )
        }

        val payment2 = Payment.create(
            paymentNumber = "PAY-002-20241107",
            orderId = 2L,
            userId = 2L,
            amount = 25000L,
            paymentMethod = PaymentMethod.CARD,
            createdBy = 2L,
            externalTransactionId = "TXN-CARD-002"
        ).let {
            Payment(
                id = idGenerator.getAndIncrement(),
                paymentNumber = it.paymentNumber,
                orderId = it.orderId,
                userId = it.userId,
                amount = it.amount,
                paymentMethod = it.paymentMethod,
                status = PaymentStatus.PROCESSING,
                externalTransactionId = it.externalTransactionId,
                failureReason = null
            )
        }

        val payment3 = Payment.create(
            paymentNumber = "PAY-003-20241107",
            orderId = 3L,
            userId = 1L,
            amount = 50000L,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            createdBy = 1L
        ).let {
            Payment(
                id = idGenerator.getAndIncrement(),
                paymentNumber = it.paymentNumber,
                orderId = it.orderId,
                userId = it.userId,
                amount = it.amount,
                paymentMethod = it.paymentMethod,
                status = PaymentStatus.PENDING,
                externalTransactionId = null,
                failureReason = null
            )
        }

        payments[payment1.id] = payment1
        payments[payment2.id] = payment2
        payments[payment3.id] = payment3
    }

    /**
     * 결제 정보를 저장하거나 업데이트한다
     *
     * @param payment 저장할 결제 엔티티
     * @return 저장된 결제 엔티티 (ID가 할당된 상태)
     */
    override fun save(payment: Payment): Payment {
        simulateLatency()

        val savedPayment = if (payment.id == 0L) {
            Payment(
                id = idGenerator.getAndIncrement(),
                paymentNumber = payment.paymentNumber,
                orderId = payment.orderId,
                userId = payment.userId,
                amount = payment.amount,
                paymentMethod = payment.paymentMethod,
                status = payment.status,
                externalTransactionId = payment.externalTransactionId,
                failureReason = payment.failureReason
            )
        } else {
            payment
        }

        payments[savedPayment.id] = savedPayment
        return savedPayment
    }

    /**
     * 결제 ID로 결제 정보를 조회한다
     *
     * @param id 조회할 결제의 ID
     * @return 결제 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): Payment? {
        simulateLatency()
        return payments[id]
    }

    /**
     * 사용자 ID로 모든 결제 정보를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 모든 결제 목록
     */
    override fun findByUserId(userId: Long): List<Payment> {
        simulateLatency()
        return payments.values.filter { it.userId == userId }
    }

    /**
     * 결제 번호로 결제 정보를 조회한다
     *
     * @param paymentNumber 조회할 결제 번호
     * @return 결제 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByPaymentNumber(paymentNumber: String): Payment? {
        simulateLatency()
        return payments.values.find { it.paymentNumber == paymentNumber }
    }

    /**
     * 결제 상태로 결제 정보들을 조회한다
     *
     * @param status 조회할 결제 상태
     * @return 상태에 맞는 결제 목록
     */
    override fun findByStatus(status: PaymentStatus): List<Payment> {
        simulateLatency()
        return payments.values.filter { it.status == status }
    }

    /**
     * 외부 거래 ID로 결제 정보를 조회한다
     *
     * @param externalTransactionId 조회할 외부 거래 ID
     * @return 결제 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByExternalTransactionId(externalTransactionId: String): Payment? {
        simulateLatency()
        return payments.values.find { it.externalTransactionId == externalTransactionId }
    }

    /**
     * 주문 ID로 결제 정보들을 조회한다
     *
     * @param orderId 조회할 주문의 ID
     * @return 주문에 연관된 모든 결제 목록
     */
    override fun findByOrderId(orderId: Long): List<Payment> {
        simulateLatency()
        return payments.values.filter { it.orderId == orderId }
    }

    /**
     * 실제 데이터베이스 지연시간을 시뮤레이션한다
     */
    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        payments.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}