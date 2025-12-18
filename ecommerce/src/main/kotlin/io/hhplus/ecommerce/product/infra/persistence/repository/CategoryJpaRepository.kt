package io.hhplus.ecommerce.product.infra.persistence.repository

import io.hhplus.ecommerce.product.infra.persistence.entity.CategoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * Category JPA Repository
 */
interface CategoryJpaRepository : JpaRepository<CategoryJpaEntity, Long> {
    fun findByName(name: String): CategoryJpaEntity?

    fun existsByName(name: String): Boolean

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.deletedAt IS NULL ORDER BY c.displayOrder")
    fun findActiveCategories(): List<CategoryJpaEntity>

    @Query("SELECT c FROM CategoryJpaEntity c ORDER BY c.displayOrder")
    fun findAllByOrderByDisplayOrderAsc(): List<CategoryJpaEntity>
}