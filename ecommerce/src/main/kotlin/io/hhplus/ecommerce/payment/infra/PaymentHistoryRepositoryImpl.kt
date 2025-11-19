package io.hhplus.ecommerce.payment.infra

import io.hhplus.ecommerce.payment.domain.entity.PaymentHistory
import io.hhplus.ecommerce.payment.domain.repository.PaymentHistoryRepository
import io.hhplus.ecommerce.payment.infra.mapper.PaymentHistoryMapper
import io.hhplus.ecommerce.payment.infra.mapper.toDomain
import io.hhplus.ecommerce.payment.infra.mapper.toEntity
import io.hhplus.ecommerce.payment.infra.persistence.repository.PaymentHistoryJpaRepository
import org.springframework.stereotype.Repository

/**
 * PaymentHistory Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class PaymentHistoryRepositoryImpl(
    private val jpaRepository: PaymentHistoryJpaRepository,
    private val mapper: PaymentHistoryMapper
) : PaymentHistoryRepository {

    /**
     * PaymentHistory 저장
     */
    override fun save(paymentHistory: PaymentHistory): PaymentHistory {
        return jpaRepository.save(paymentHistory.toEntity(mapper)).toDomain(mapper)!!
    }

    override fun findById(id: Long): PaymentHistory? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByPaymentId(paymentId: Long): List<PaymentHistory> =
        jpaRepository.findByPaymentId(paymentId).toDomain(mapper)

    override fun findByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentHistory> =
        jpaRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).toDomain(mapper)

    override fun findByStatusAfter(statusAfter: String): List<PaymentHistory> =
        jpaRepository.findByStatusAfter(statusAfter).toDomain(mapper)

    override fun findByPaymentIdAndStatusAfter(paymentId: Long, statusAfter: String): List<PaymentHistory> =
        jpaRepository.findByPaymentIdAndStatusAfter(paymentId, statusAfter).toDomain(mapper)
}
