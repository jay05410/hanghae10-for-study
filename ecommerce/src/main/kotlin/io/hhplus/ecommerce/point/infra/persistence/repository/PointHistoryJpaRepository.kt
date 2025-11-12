package io.hhplus.ecommerce.point.infra.persistence.repository

import io.hhplus.ecommerce.point.infra.persistence.entity.PointHistoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * PointHistory Spring Data JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 쿼리 메서드 제공
 */
@Repository
interface PointHistoryJpaRepository : JpaRepository<PointHistoryJpaEntity, Long> {

    fun findByUserId(userId: Long): List<PointHistoryJpaEntity>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<PointHistoryJpaEntity>
}
