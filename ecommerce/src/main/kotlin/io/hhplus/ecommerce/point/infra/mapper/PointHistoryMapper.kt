package io.hhplus.ecommerce.point.infra.mapper

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.infra.persistence.entity.PointHistoryJpaEntity
import org.springframework.stereotype.Component

/**
 * PointHistory 도메인 모델 <-> JPA 엔티티 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 양방향 변환
 * - 도메인 계층과 인프라 계층의 분리 유지
 */
@Component
class PointHistoryMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: PointHistoryJpaEntity): PointHistory {
        return PointHistory(
            id = entity.id,
            userId = entity.userId,
            amount = entity.amount,
            transactionType = entity.transactionType,
            balanceBefore = entity.balanceBefore,
            balanceAfter = entity.balanceAfter,
            orderId = entity.orderId,
            description = entity.description,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            createdBy = entity.createdBy ?: 0,
            updatedBy = entity.updatedBy ?: 0,
            deletedAt = entity.deletedAt
        )
    }

    /**
     * 도메인 모델 -> JPA 엔티티 변환
     */
    fun toEntity(domain: PointHistory): PointHistoryJpaEntity {
        return PointHistoryJpaEntity(
            id = domain.id,
            userId = domain.userId,
            amount = domain.amount,
            transactionType = domain.transactionType,
            balanceBefore = domain.balanceBefore,
            balanceAfter = domain.balanceAfter,
            orderId = domain.orderId,
            description = domain.description
        ).apply {
            isActive = domain.isActive
            createdAt = domain.createdAt
            updatedAt = domain.updatedAt
            createdBy = domain.createdBy
            updatedBy = domain.updatedBy
            deletedAt = domain.deletedAt
        }
    }

    /**
     * JPA 엔티티 리스트 -> 도메인 모델 리스트 변환
     */
    fun toDomainList(entities: List<PointHistoryJpaEntity>): List<PointHistory> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트 -> JPA 엔티티 리스트 변환
     */
    fun toEntityList(domains: List<PointHistory>): List<PointHistoryJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * PointHistory Mapper Extension Functions
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
fun PointHistoryJpaEntity?.toDomain(mapper: PointHistoryMapper): PointHistory? =
    this?.let { mapper.toDomain(it) }

fun PointHistory.toEntity(mapper: PointHistoryMapper): PointHistoryJpaEntity =
    mapper.toEntity(this)

fun List<PointHistoryJpaEntity>.toDomain(mapper: PointHistoryMapper): List<PointHistory> =
    map { mapper.toDomain(it) }
