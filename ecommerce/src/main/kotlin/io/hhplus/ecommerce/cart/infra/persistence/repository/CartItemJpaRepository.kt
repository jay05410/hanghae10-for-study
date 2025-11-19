package io.hhplus.ecommerce.cart.infra.persistence.repository

import io.hhplus.ecommerce.cart.infra.persistence.entity.CartItemJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * CartItem JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 커스텀 쿼리 메서드 제공
 *
 * 주의:
 * - 이 인터페이스는 JPA 엔티티를 다룹니다
 * - 도메인 모델 변환은 CartItemRepositoryImpl에서 처리합니다
 */
interface CartItemJpaRepository : JpaRepository<CartItemJpaEntity, Long> {

    /**
     * 장바구니 ID로 모든 활성 아이템 조회
     */
    @Query("SELECT ci FROM CartItemJpaEntity ci WHERE ci.cartId = :cartId AND ci.deletedAt IS NULL")
    fun findActiveByCartId(@Param("cartId") cartId: Long): List<CartItemJpaEntity>

    /**
     * 장바구니 ID로 모든 아이템 조회 (활성 여부 무관)
     */
    @Query("SELECT ci FROM CartItemJpaEntity ci WHERE ci.cartId = :cartId")
    fun findByCartId(@Param("cartId") cartId: Long): List<CartItemJpaEntity>

    /**
     * 장바구니 ID와 상품 ID로 아이템 조회
     */
    @Query("SELECT ci FROM CartItemJpaEntity ci WHERE ci.cartId = :cartId AND ci.productId = :productId")
    fun findByCartIdAndProductId(@Param("cartId") cartId: Long, @Param("productId") productId: Long): CartItemJpaEntity?

    /**
     * 장바구니 ID로 모든 아이템 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItemJpaEntity ci WHERE ci.cartId = :cartId")
    fun deleteByCartId(@Param("cartId") cartId: Long)

    /**
     * ID 목록으로 아이템 일괄 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItemJpaEntity ci WHERE ci.id IN :ids")
    fun deleteByIdIn(@Param("ids") ids: Collection<Long>): Int
}
