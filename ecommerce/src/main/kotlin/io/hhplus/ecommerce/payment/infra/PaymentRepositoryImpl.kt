package io.hhplus.ecommerce.payment.infra

import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.repository.PaymentRepository
import io.hhplus.ecommerce.payment.infra.mapper.PaymentMapper
import io.hhplus.ecommerce.payment.infra.mapper.toDomain
import io.hhplus.ecommerce.payment.infra.mapper.toEntity
import io.hhplus.ecommerce.payment.infra.persistence.repository.PaymentJpaRepository
import org.springframework.stereotype.Repository

/**
 * Payment Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class PaymentRepositoryImpl(
    private val jpaRepository: PaymentJpaRepository,
    private val mapper: PaymentMapper
) : PaymentRepository {

    override fun save(payment: Payment): Payment =
        jpaRepository.save(payment.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): Payment? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByPaymentNumber(paymentNumber: String): Payment? =
        jpaRepository.findByPaymentNumber(paymentNumber).toDomain(mapper)

    override fun findByOrderId(orderId: Long): List<Payment> =
        jpaRepository.findByOrderId(orderId).toDomain(mapper)

    override fun findByUserId(userId: Long): List<Payment> =
        jpaRepository.findByUserId(userId).toDomain(mapper)

    override fun findByStatus(status: PaymentStatus): List<Payment> =
        jpaRepository.findByStatus(status).toDomain(mapper)

    override fun findByExternalTransactionId(externalTransactionId: String): Payment? =
        jpaRepository.findByExternalTransactionId(externalTransactionId).toDomain(mapper)

    override fun findPaymentWithHistoriesByPaymentNumber(paymentNumber: String): Payment? {
        return jpaRepository.findPaymentWithHistoriesByPaymentNumber(paymentNumber).toDomain(mapper)
    }

    override fun findPaymentWithHistoriesById(paymentId: Long): Payment? {
        return jpaRepository.findPaymentWithHistoriesById(paymentId).toDomain(mapper)
    }
}
