package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Repository
class InMemoryProductStatisticsRepository : ProductStatisticsRepository {
    private val statistics = ConcurrentHashMap<Long, ProductStatistics>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val stats1 = ProductStatistics.create(
            productId = 1L,
            createdBy = 1L
        ).let {
            ProductStatistics(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                viewCount = 150L,
                salesCount = 45L,
                version = 0
            )
        }

        val stats2 = ProductStatistics.create(
            productId = 2L,
            createdBy = 1L
        ).let {
            ProductStatistics(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                viewCount = 230L,
                salesCount = 67L,
                version = 0
            )
        }

        val stats3 = ProductStatistics.create(
            productId = 3L,
            createdBy = 1L
        ).let {
            ProductStatistics(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                viewCount = 89L,
                salesCount = 23L,
                version = 0
            )
        }

        val stats4 = ProductStatistics.create(
            productId = 4L,
            createdBy = 1L
        ).let {
            ProductStatistics(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                viewCount = 345L,
                salesCount = 123L,
                version = 0
            )
        }

        statistics[stats1.id] = stats1
        statistics[stats2.id] = stats2
        statistics[stats3.id] = stats3
        statistics[stats4.id] = stats4
    }

    override fun findByProductId(productId: Long): ProductStatistics? {
        simulateLatency()
        return statistics.values.find { it.productId == productId }
    }

    override fun findByProductIdWithLock(productId: Long): ProductStatistics? {
        simulateLatency()
        return findByProductId(productId)
    }

    override fun findTopPopularProducts(limit: Int): List<ProductStatistics> {
        simulateLatency()
        return statistics.values
            .sortedByDescending { it.getPopularityScore() }
            .take(limit)
    }

    override fun save(productStatistics: ProductStatistics): ProductStatistics {
        simulateLatency()

        val savedStatistics = if (productStatistics.id == 0L) {
            ProductStatistics(
                id = idGenerator.getAndIncrement(),
                productId = productStatistics.productId,
                viewCount = productStatistics.viewCount,
                salesCount = productStatistics.salesCount,
                version = productStatistics.version
            )
        } else {
            productStatistics
        }
        statistics[savedStatistics.id] = savedStatistics
        return savedStatistics
    }

    override fun delete(productStatistics: ProductStatistics) {
        simulateLatency()
        statistics.remove(productStatistics.id)
    }

    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    fun clear() {
        statistics.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}