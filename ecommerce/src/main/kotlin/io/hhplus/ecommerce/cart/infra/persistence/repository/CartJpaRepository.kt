package io.hhplus.ecommerce.cart.infra.persistence.repository

import io.hhplus.ecommerce.cart.infra.persistence.entity.CartJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Cart JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 커스텀 쿼리 메서드 제공
 *
 * 주의:
 * - 이 인터페이스는 JPA 엔티티를 다룹니다
 * - 도메인 모델 변환은 CartRepositoryImpl에서 처리합니다
 *
 * N+1 문제 방지:
 * - Fetch join: 단건 조회에 적합 (장바구니 상세 조회)
 */
interface CartJpaRepository : JpaRepository<CartJpaEntity, Long> {


    /**
     * 사용자 ID로 장바구니 조회 (활성 여부 무관)
     */
    fun findByUserId(userId: Long): CartJpaEntity?

    /**
     * 사용자 ID로 장바구니와 아이템을 함께 조회 (단건)
     *
     * Fetch join 사용 이유:
     * - 단건 조회 (각 사용자는 하나의 장바구니만 가짐)
     * - 페이지네이션 불필요
     * - 명시적인 쿼리 제어 가능
     */
    @Query("SELECT c FROM CartJpaEntity c LEFT JOIN FETCH c.cartItems WHERE c.userId = :userId AND c.deletedAt IS NULL")
    fun findByUserIdWithItems(@Param("userId") userId: Long): CartJpaEntity?

    /**
     * 장바구니 ID로 장바구니와 아이템을 함께 조회 (단건)
     *
     * Fetch join 사용 이유:
     * - 단건 조회이므로 페이지네이션 불필요
     * - 명시적인 쿼리 제어 가능
     */
    @Query("SELECT c FROM CartJpaEntity c LEFT JOIN FETCH c.cartItems WHERE c.id = :cartId")
    fun findCartWithItemsById(@Param("cartId") cartId: Long): CartJpaEntity?
}
