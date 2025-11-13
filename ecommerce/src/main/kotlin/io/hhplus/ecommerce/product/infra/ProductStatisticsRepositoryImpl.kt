package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import io.hhplus.ecommerce.product.infra.persistence.repository.ProductStatisticsJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

/**
 * ProductStatistics Repository JPA 구현체
 */
@Repository
class ProductStatisticsRepositoryImpl(
    private val jpaRepository: ProductStatisticsJpaRepository
) : ProductStatisticsRepository {

    override fun findByProductId(productId: Long): ProductStatistics? =
        jpaRepository.findByProductId(productId)

    override fun findByProductIdWithLock(productId: Long): ProductStatistics? =
        jpaRepository.findByProductIdWithLock(productId)

    override fun findTopPopularProducts(limit: Int): List<ProductStatistics> {
        val pageable = PageRequest.of(0, limit)
        return jpaRepository.findTopPopularProducts(pageable)
    }

    override fun save(productStatistics: ProductStatistics): ProductStatistics =
        jpaRepository.save(productStatistics)

    override fun delete(productStatistics: ProductStatistics) {
        jpaRepository.delete(productStatistics)
    }
}