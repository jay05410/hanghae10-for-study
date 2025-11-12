package io.hhplus.ecommerce.payment.infra.mapper

import io.hhplus.ecommerce.payment.domain.entity.PaymentHistory
import io.hhplus.ecommerce.payment.infra.persistence.entity.PaymentHistoryJpaEntity
import org.springframework.stereotype.Component

/**
 * PaymentHistory 도메인 모델 ↔ JPA 엔티티 변환 Mapper
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
class PaymentHistoryMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: PaymentHistoryJpaEntity): PaymentHistory {
        return PaymentHistory(
            id = entity.id,
            paymentId = entity.paymentId,
            statusBefore = entity.statusBefore,
            statusAfter = entity.statusAfter,
            reason = entity.reason,
            pgResponse = entity.pgResponse,
            amount = entity.amount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            createdBy = entity.createdBy ?: 0,
            updatedBy = entity.updatedBy ?: 0,
            deletedAt = entity.deletedAt
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: PaymentHistory): PaymentHistoryJpaEntity {
        return PaymentHistoryJpaEntity(
            id = domain.id,
            paymentId = domain.paymentId,
            statusBefore = domain.statusBefore,
            statusAfter = domain.statusAfter,
            reason = domain.reason,
            pgResponse = domain.pgResponse,
            amount = domain.amount
        ).apply {
            createdAt = domain.createdAt
            updatedAt = domain.updatedAt
            createdBy = domain.createdBy
            updatedBy = domain.updatedBy
            deletedAt = domain.deletedAt
        }
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<PaymentHistoryJpaEntity>): List<PaymentHistory> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<PaymentHistory>): List<PaymentHistoryJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * PaymentHistory Mapper Extension Functions
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
fun PaymentHistoryJpaEntity?.toDomain(mapper: PaymentHistoryMapper): PaymentHistory? =
    this?.let { mapper.toDomain(it) }

fun PaymentHistory.toEntity(mapper: PaymentHistoryMapper): PaymentHistoryJpaEntity =
    mapper.toEntity(this)

fun List<PaymentHistoryJpaEntity>.toDomain(mapper: PaymentHistoryMapper): List<PaymentHistory> =
    map { mapper.toDomain(it) }
