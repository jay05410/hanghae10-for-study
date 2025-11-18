package io.hhplus.ecommerce.point.infra.mapper

import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.infra.persistence.entity.UserPointJpaEntity
import org.springframework.stereotype.Component

/**
 * UserPoint 도메인 모델 <-> JPA 엔티티 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 양방향 변환
 * - 도메인 계층과 인프라 계층의 분리 유지
 */
@Component
class UserPointMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: UserPointJpaEntity): UserPoint {
        return UserPoint(
            id = entity.id,
            userId = entity.userId,
            balance = Balance.of(entity.balance),
            version = entity.version,
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
    fun toEntity(domain: UserPoint): UserPointJpaEntity {
        return UserPointJpaEntity(
            id = domain.id,
            userId = domain.userId,
            balance = domain.balance.value,
            version = domain.version
        ).apply {
            createdBy = domain.createdBy
            updatedBy = domain.updatedBy
            deletedAt = domain.deletedAt
        }
    }

    /**
     * JPA 엔티티 리스트 -> 도메인 모델 리스트 변환
     */
    fun toDomainList(entities: List<UserPointJpaEntity>): List<UserPoint> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트 -> JPA 엔티티 리스트 변환
     */
    fun toEntityList(domains: List<UserPoint>): List<UserPointJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * UserPoint Mapper Extension Functions
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
fun UserPointJpaEntity?.toDomain(mapper: UserPointMapper): UserPoint? =
    this?.let { mapper.toDomain(it) }

fun UserPoint.toEntity(mapper: UserPointMapper): UserPointJpaEntity =
    mapper.toEntity(this)

fun List<UserPointJpaEntity>.toDomain(mapper: UserPointMapper): List<UserPoint> =
    map { mapper.toDomain(it) }
