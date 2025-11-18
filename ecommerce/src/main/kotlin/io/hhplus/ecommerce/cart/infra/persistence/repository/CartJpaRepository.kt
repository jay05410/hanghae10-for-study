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
 */
interface CartJpaRepository : JpaRepository<CartJpaEntity, Long> {

    /**
     * 사용자 ID로 활성 상태의 장바구니 조회
     */
    @Query("SELECT c FROM CartJpaEntity c WHERE c.userId = :userId AND c.deletedAt IS NULL")
    fun findByUserIdAndIsActive(@Param("userId") userId: Long, @Param("isActive") isActive: Boolean = true): CartJpaEntity?

    /**
     * 사용자 ID로 장바구니 조회 (활성 여부 무관)
     */
    fun findByUserId(userId: Long): CartJpaEntity?

    /**
     * 사용자 ID로 장바구니와 아이템을 함께 조회 (Fetch Join)
     */
    @Query("SELECT c FROM CartJpaEntity c LEFT JOIN FETCH c.cartItems WHERE c.userId = :userId AND c.deletedAt IS NULL")
    fun findByUserIdWithItems(@Param("userId") userId: Long): CartJpaEntity?

    /**
     * 장바구니 ID로 장바구니와 아이템을 함께 조회
     */
    @Query("SELECT c FROM CartJpaEntity c LEFT JOIN FETCH c.cartItems WHERE c.id = :cartId")
    fun findCartWithItemsById(@Param("cartId") cartId: Long): CartJpaEntity?
}
