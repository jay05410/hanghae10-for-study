package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.entity.Category
import io.hhplus.ecommerce.product.domain.repository.CategoryRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Repository
class InMemoryCategoryRepository : CategoryRepository {
    private val categories = ConcurrentHashMap<Long, Category>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val greenTea = Category.create(
            name = "녹차",
            description = "신선하고 깔끔한 녹차 제품들",
            displayOrder = 1,
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val blackTea = Category.create(
            name = "홍차",
            description = "진하고 풍부한 맛의 홍차 제품들",
            displayOrder = 2,
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val herbalTea = Category.create(
            name = "허브차",
            description = "자연의 향을 담은 허브차 제품들",
            displayOrder = 3,
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val oolongTea = Category.create(
            name = "우롱차",
            description = "은은한 발효의 맛이 특징인 우롱차",
            displayOrder = 4,
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        categories[greenTea.id] = greenTea
        categories[blackTea.id] = blackTea
        categories[herbalTea.id] = herbalTea
        categories[oolongTea.id] = oolongTea
    }

    override fun save(category: Category): Category {
        simulateLatency()

        val savedCategory = if (category.id == 0L) {
            Category(
                id = idGenerator.getAndIncrement(),
                name = category.name,
                description = category.description,
                displayOrder = category.displayOrder
            )
        } else {
            category
        }

        categories[savedCategory.id] = savedCategory
        return savedCategory
    }

    override fun findById(id: Long): Category? {
        simulateLatency()
        return categories[id]
    }

    override fun findByName(name: String): Category? {
        simulateLatency()
        return categories.values.find { it.name == name }
    }

    override fun findAll(): List<Category> {
        simulateLatency()
        return categories.values.toList().sortedBy { it.displayOrder }
    }

    override fun findActiveCategories(): List<Category> {
        simulateLatency()
        return categories.values
            .filter { it.isAvailable() }
            .sortedBy { it.displayOrder }
    }

    override fun findByDisplayOrder(): List<Category> {
        simulateLatency()
        return categories.values.toList().sortedBy { it.displayOrder }
    }

    override fun existsByName(name: String): Boolean {
        simulateLatency()
        return categories.values.any { it.name == name }
    }

    override fun deleteById(id: Long) {
        simulateLatency()
        categories.remove(id)
    }

    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    fun clear() {
        categories.clear()
        idGenerator.set(1)
        initializeSampleData()
    }

    private fun Category.copy(id: Long = this.id): Category {
        return Category(
            id = id,
            name = this.name,
            description = this.description,
            displayOrder = this.displayOrder
        )
    }
}