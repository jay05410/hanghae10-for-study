package io.hhplus.ecommerce.product.infra.persistence.repository

import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.product.infra.persistence.entity.ProductJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Product JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 상품 영속성 처리
 * - 기본 CRUD 및 상품 정의 쿼리 제공
 *
 * 책임:
 * - 상품 엔티티 저장/조회/수정/삭제
 * - 카테고리별, 상태별 상품 조회
 * - 활성 상품 조회 및 검색
 */
interface ProductJpaRepository : JpaRepository<ProductJpaEntity, Long> {

    /**
     * ID와 활성 상태로 상품 조회
     */
    @Query("SELECT p FROM ProductJpaEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findByIdAndIsActive(@Param("id") id: Long, @Param("isActive") isActive: Boolean): ProductJpaEntity?

    /**
     * 활성 상태로 상품 목록 조회
     */
    @Query("SELECT p FROM ProductJpaEntity p WHERE p.deletedAt IS NULL")
    fun findAllByIsActive(@Param("isActive") isActive: Boolean): List<ProductJpaEntity>

    /**
     * 상품 상태로 상품 목록 조회
     */
    fun findByStatus(status: ProductStatus): List<ProductJpaEntity>

    /**
     * 카테고리 ID와 활성 상태로 상품 목록 조회
     */
    @Query("SELECT p FROM ProductJpaEntity p WHERE p.categoryId = :categoryId AND p.deletedAt IS NULL")
    fun findByCategoryIdAndIsActive(@Param("categoryId") categoryId: Long, @Param("isActive") isActive: Boolean): List<ProductJpaEntity>

    /**
     * 상품명 검색 (부분 일치)
     */
    fun findByNameContaining(keyword: String): List<ProductJpaEntity>

    /**
     * 활성 상품 전체 조회 (deletedAt이 null인 경우만)
     */
    @Query("SELECT p FROM ProductJpaEntity p WHERE p.deletedAt IS NULL")
    fun findActiveProducts(): List<ProductJpaEntity>
}
