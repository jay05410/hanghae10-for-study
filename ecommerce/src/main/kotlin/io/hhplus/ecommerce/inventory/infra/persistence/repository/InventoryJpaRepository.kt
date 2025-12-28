package io.hhplus.ecommerce.inventory.infra.persistence.repository

import io.hhplus.ecommerce.inventory.infra.persistence.entity.InventoryJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Inventory Spring Data JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 쿼리 메서드 제공
 * - 비관적 락을 사용한 동시성 제어
 */
@Repository
interface InventoryJpaRepository : JpaRepository<InventoryJpaEntity, Long> {
    /**
     * 상품 ID로 재고 조회
     *
     * @param productId 상품 ID
     * @return 재고 JPA 엔티티
     */
    fun findByProductId(productId: Long): InventoryJpaEntity?

    /**
     * 상품 ID로 재고 조회 (비관적 락 사용)
     *
     * PESSIMISTIC_FORCE_INCREMENT를 사용하여:
     * 1. SELECT FOR UPDATE로 락 획득
     * 2. 버전을 강제로 증가시켜 다른 트랜잭션의 낙관적 락 충돌 방지
     * 3. JPA 캐시와 관계없이 항상 최신 데이터 반환
     *
     * @param productId 상품 ID
     * @return 재고 JPA 엔티티
     */
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    @Query("SELECT i FROM InventoryJpaEntity i WHERE i.productId = :productId")
    fun findByProductIdWithLock(@Param("productId") productId: Long): InventoryJpaEntity?

    /**
     * 활성 상태인 모든 재고 조회
     *
     * @return 활성 재고 JPA 엔티티 리스트
     */
    @Query("SELECT i FROM InventoryJpaEntity i WHERE i.deletedAt IS NULL")
    fun findAllActive(): List<InventoryJpaEntity>
}
