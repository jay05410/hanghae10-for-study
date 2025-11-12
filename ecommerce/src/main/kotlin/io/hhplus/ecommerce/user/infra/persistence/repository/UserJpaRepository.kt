package io.hhplus.ecommerce.user.infra.persistence.repository

import io.hhplus.ecommerce.user.infra.persistence.entity.UserJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * User JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 사용자 영속성 처리
 * - 기본 CRUD 및 사용자 정의 쿼리 제공
 *
 * 책임:
 * - 사용자 엔티티 저장/조회/수정/삭제
 * - 이메일, 로그인 ID 기반 조회 및 중복 검증
 * - 활성 사용자 조회
 */
interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {

    /**
     * 로그인 ID로 사용자 조회
     */
    fun findByLoginId(loginId: String): UserJpaEntity?

    /**
     * 이메일로 사용자 조회
     */
    fun findByEmail(email: String): UserJpaEntity?

    /**
     * 로그인 ID 존재 여부 확인
     */
    fun existsByLoginId(loginId: String): Boolean

    /**
     * 이메일 존재 여부 확인
     */
    fun existsByEmail(email: String): Boolean

    /**
     * 활성 사용자 전체 조회
     */
    fun findByIsActiveTrue(): List<UserJpaEntity>

    /**
     * 활성 사용자 전체 조회 (deletedAt이 null인 경우만)
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.isActive = true AND u.deletedAt IS NULL")
    fun findActiveUsers(): List<UserJpaEntity>
}
