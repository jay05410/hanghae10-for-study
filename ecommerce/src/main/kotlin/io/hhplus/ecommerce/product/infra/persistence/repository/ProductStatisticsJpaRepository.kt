package io.hhplus.ecommerce.product.infra.persistence.repository

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductStatisticsJpaRepository : JpaRepository<ProductStatistics, Long> {

    fun findByProductId(productId: Long): ProductStatistics?

    @Query("""
        SELECT ps FROM ProductStatistics ps
        ORDER BY (ps.totalSalesCount * 0.7 + ps.totalViewCount * 0.3) DESC
    """)
    fun findTopPopularProducts(limit: Int): List<ProductStatistics>
}