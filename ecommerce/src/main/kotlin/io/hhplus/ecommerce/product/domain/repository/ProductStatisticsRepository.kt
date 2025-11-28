package io.hhplus.ecommerce.product.domain.repository

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics

interface ProductStatisticsRepository {
    fun findByProductId(productId: Long): ProductStatistics?
    fun findTopPopularProducts(limit: Int): List<ProductStatistics>
    fun save(productStatistics: ProductStatistics): ProductStatistics
    fun bulkUpdateViewCounts(updates: Map<Long, Long>)
    fun bulkUpdateSalesCounts(updates: Map<Long, Long>)
}