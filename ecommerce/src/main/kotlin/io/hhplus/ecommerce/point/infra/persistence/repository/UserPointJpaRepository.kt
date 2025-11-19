package io.hhplus.ecommerce.point.infra.persistence.repository

import io.hhplus.ecommerce.point.infra.persistence.entity.UserPointJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
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
 *
 * N+1 문제 방지:
 * - EntityGraph: 페이지네이션 지원, 런타임 유연성 (포인트 이력 조회)
 */
@Repository
interface UserPointJpaRepository : JpaRepository<UserPointJpaEntity, Long> {

    fun findByUserId(userId: Long): UserPointJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT up FROM UserPointJpaEntity up WHERE up.userId = :userId")
    fun findByUserIdWithLock(@Param("userId") userId: Long): UserPointJpaEntity?

    /**
     * 사용자의 포인트 정보를 이력과 함께 조회
     *
     * EntityGraph 사용 이유:
     * - 페이지네이션 지원 (향후 이력 목록 페이징 가능)
     * - Fetch join은 페이지네이션과 함께 사용 시 메모리 페이징 발생
     * - 포인트 이력은 시간이 지나면서 계속 증가하므로 페이징이 필요할 수 있음
     */
    @EntityGraph(attributePaths = ["pointHistories"])
    @Query("SELECT up FROM UserPointJpaEntity up WHERE up.userId = :userId")
    fun findUserPointWithHistoriesByUserId(@Param("userId") userId: Long): UserPointJpaEntity?
}
