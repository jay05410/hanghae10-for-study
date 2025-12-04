package io.hhplus.ecommerce.user.infra.persistence.adapter

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.infra.persistence.mapper.UserMapper
import io.hhplus.ecommerce.user.infra.persistence.mapper.toDomain
import io.hhplus.ecommerce.user.infra.persistence.mapper.toEntity
import io.hhplus.ecommerce.user.infra.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Repository

/**
 * User Repository JPA 구현체 - 인프라 계층 (Adapter)
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 */
@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
    private val mapper: UserMapper
) : UserRepository {

    override fun save(user: User): User =
        jpaRepository.save(user.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): User? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByLoginId(loginId: String): User? =
        jpaRepository.findByLoginId(loginId).toDomain(mapper)

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email).toDomain(mapper)

    override fun existsByLoginId(loginId: String): Boolean =
        jpaRepository.existsByLoginId(loginId)

    override fun existsByEmail(email: String): Boolean =
        jpaRepository.existsByEmail(email)

    override fun findActiveUsers(): List<User> =
        jpaRepository.findActiveUsers().toDomain(mapper)

    override fun findByIsActiveTrue(): List<User> =
        jpaRepository.findActiveUsers().toDomain(mapper)
}
