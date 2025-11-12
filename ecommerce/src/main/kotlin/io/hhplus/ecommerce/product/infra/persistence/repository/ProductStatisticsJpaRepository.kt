package io.hhplus.ecommerce.product.infra.persistence.repository

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.LockModeType

interface ProductStatisticsJpaRepository : JpaRepository<ProductStatistics, Long> {

    fun findByProductId(productId: Long): ProductStatistics?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductStatistics p WHERE p.productId = :productId")
    fun findByProductIdWithLock(productId: Long): ProductStatistics?

    @Query("SELECT p FROM ProductStatistics p ORDER BY (p.viewCount * 0.3 + p.salesCount * 0.7) DESC")
    fun findTopPopularProducts(limit: Int): List<ProductStatistics>
}