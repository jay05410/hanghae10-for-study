package io.hhplus.ecommerce.point.infra.persistence.repository

import io.hhplus.ecommerce.point.infra.persistence.entity.UserPointJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * UserPoint Spring Data JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 쿼리 메서드 제공
 * - Pessimistic Lock을 통한 동시성 제어
 */
@Repository
interface UserPointJpaRepository : JpaRepository<UserPointJpaEntity, Long> {

    fun findByUserId(userId: Long): UserPointJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT up FROM UserPointJpaEntity up WHERE up.userId = :userId")
    fun findByUserIdWithLock(@Param("userId") userId: Long): UserPointJpaEntity?

    @Query("SELECT up FROM UserPointJpaEntity up LEFT JOIN FETCH up.pointHistories WHERE up.userId = :userId ORDER BY up.pointHistories.createdAt DESC")
    fun findUserPointWithHistoriesByUserId(@Param("userId") userId: Long): UserPointJpaEntity?
}
