package io.hhplus.ecommerce.point.infra

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.infra.mapper.PointHistoryMapper
import io.hhplus.ecommerce.point.infra.mapper.toDomain
import io.hhplus.ecommerce.point.infra.mapper.toEntity
import io.hhplus.ecommerce.point.infra.persistence.repository.PointHistoryJpaRepository
import org.springframework.stereotype.Repository

/**
 * PointHistory Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 */
@Repository
class PointHistoryRepositoryImpl(
    private val jpaRepository: PointHistoryJpaRepository,
    private val mapper: PointHistoryMapper
) : PointHistoryRepository {

    override fun save(pointHistory: PointHistory): PointHistory =
        jpaRepository.save(pointHistory.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): PointHistory? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByUserId(userId: Long): List<PointHistory> =
        jpaRepository.findByUserId(userId).toDomain(mapper)

    override fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<PointHistory> =
        jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).toDomain(mapper)

    override fun delete(pointHistory: PointHistory) {
        jpaRepository.deleteById(pointHistory.id)
    }
}
