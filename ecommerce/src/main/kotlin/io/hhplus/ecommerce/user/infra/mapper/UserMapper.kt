package io.hhplus.ecommerce.user.infra.mapper

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.infra.persistence.entity.UserJpaEntity
import org.springframework.stereotype.Component

/**
 * User 도메인 모델 ↔ JPA 엔티티 변환 Mapper
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
class UserMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: UserJpaEntity): User {
        return User(
            id = entity.id,
            loginType = entity.loginType,
            loginId = entity.loginId,
            password = entity.password,
            email = entity.email,
            name = entity.name,
            phone = entity.phone,
            providerId = entity.providerId,
            isActive = !entity.isDeleted(),
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
    fun toEntity(domain: User): UserJpaEntity {
        return UserJpaEntity(
            id = domain.id,
            loginType = domain.loginType,
            loginId = domain.loginId,
            password = domain.password,
            email = domain.email,
            name = domain.name,
            phone = domain.phone,
            providerId = domain.providerId
        ).apply {
            if (!domain.isActive) { delete() }
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
    fun toDomainList(entities: List<UserJpaEntity>): List<User> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<User>): List<UserJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * User Mapper Extension Functions
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
fun UserJpaEntity?.toDomain(mapper: UserMapper): User? =
    this?.let { mapper.toDomain(it) }

fun User.toEntity(mapper: UserMapper): UserJpaEntity =
    mapper.toEntity(this)

fun List<UserJpaEntity>.toDomain(mapper: UserMapper): List<User> =
    map { mapper.toDomain(it) }
