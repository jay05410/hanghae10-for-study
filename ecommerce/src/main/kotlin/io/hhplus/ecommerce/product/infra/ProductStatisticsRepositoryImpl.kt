package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import io.hhplus.ecommerce.product.infra.persistence.repository.ProductStatisticsJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductStatisticsRepositoryImpl(
    private val productStatisticsJpaRepository: ProductStatisticsJpaRepository
) : ProductStatisticsRepository {

    override fun findByProductId(productId: Long): ProductStatistics? {
        return productStatisticsJpaRepository.findByProductId(productId)
    }

    override fun findTopPopularProducts(limit: Int): List<ProductStatistics> {
        val pageable = PageRequest.of(0, limit)
        return productStatisticsJpaRepository.findAll(pageable).content
    }

    override fun save(productStatistics: ProductStatistics): ProductStatistics {
        return productStatisticsJpaRepository.save(productStatistics)
    }

    @Transactional
    override fun bulkUpdateViewCounts(updates: Map<Long, Long>) {
        updates.forEach { (productId, viewCount) ->
            val statistics = findByProductId(productId)
                ?: ProductStatistics.create(productId)

            statistics.addViewCount(viewCount)
            save(statistics)
        }
    }

    @Transactional
    override fun bulkUpdateSalesCounts(updates: Map<Long, Long>) {
        updates.forEach { (productId, salesCount) ->
            val statistics = findByProductId(productId)
                ?: ProductStatistics.create(productId)

            statistics.addSalesCount(salesCount)
            save(statistics)
        }
    }
}