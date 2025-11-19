package io.hhplus.ecommerce.payment.infra.mapper

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.infra.persistence.entity.PaymentJpaEntity
import org.springframework.stereotype.Component

/**
 * Payment 도메인 모델 ↔ JPA 엔티티 변환 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 변환 담당
 * - 도메인 계층과 인프라 계층 간의 격리 유지
 *
 * 책임:
 * - toDomain: JPA 엔티티 → 도메인 모델
 * - toEntity: 도메인 모델 → JPA 엔티티
 */
@Component
class PaymentMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: PaymentJpaEntity): Payment {
        return Payment(
            id = entity.id,
            paymentNumber = entity.paymentNumber,
            orderId = entity.orderId,
            userId = entity.userId,
            amount = entity.amount,
            paymentMethod = entity.paymentMethod,
            status = entity.status,
            externalTransactionId = entity.externalTransactionId,
            failureReason = entity.failureReason
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: Payment): PaymentJpaEntity {
        return PaymentJpaEntity(
            id = domain.id,
            paymentNumber = domain.paymentNumber,
            orderId = domain.orderId,
            userId = domain.userId,
            amount = domain.amount,
            paymentMethod = domain.paymentMethod,
            status = domain.status,
            externalTransactionId = domain.externalTransactionId,
            failureReason = domain.failureReason
        )
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<PaymentJpaEntity>): List<Payment> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<Payment>): List<PaymentJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Payment Mapper Extension Functions
 *
 * 역할:
 * - Mapper 호출을 간결하게 만들어 가독성 향상
 * - Nullable 처리를 자동화
 *
 * 사용법:
 * - entity.toDomain(mapper)  // JPA Entity → Domain
 * - domain.toEntity(mapper)   // Domain → JPA Entity
 * - entities.toDomain(mapper) // List 변환
 */
fun PaymentJpaEntity?.toDomain(mapper: PaymentMapper): Payment? =
    this?.let { mapper.toDomain(it) }

fun Payment.toEntity(mapper: PaymentMapper): PaymentJpaEntity =
    mapper.toEntity(this)

fun List<PaymentJpaEntity>.toDomain(mapper: PaymentMapper): List<Payment> =
    map { mapper.toDomain(it) }
