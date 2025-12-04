package io.hhplus.ecommerce.point.infra.persistence.adapter

import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.infra.persistence.mapper.UserPointMapper
import io.hhplus.ecommerce.point.infra.persistence.mapper.toDomain
import io.hhplus.ecommerce.point.infra.persistence.mapper.toEntity
import io.hhplus.ecommerce.point.infra.persistence.repository.UserPointJpaRepository
import org.springframework.stereotype.Repository

/**
 * UserPoint Repository 어댑터 - Infrastructure Layer
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 */
@Repository
class UserPointRepositoryImpl(
    private val jpaRepository: UserPointJpaRepository,
    private val mapper: UserPointMapper
) : UserPointRepository {

    override fun save(userPoint: UserPoint): UserPoint =
        jpaRepository.save(userPoint.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): UserPoint? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByUserId(userId: Long): UserPoint? =
        jpaRepository.findByUserId(userId).toDomain(mapper)

    override fun delete(userPoint: UserPoint) {
        jpaRepository.deleteById(userPoint.id)
    }

    override fun findUserPointWithHistoriesByUserId(userId: Long): UserPoint? {
        return jpaRepository.findUserPointWithHistoriesByUserId(userId).toDomain(mapper)
    }
}
