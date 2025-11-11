package io.hhplus.ecommerce.order.infra

import io.hhplus.ecommerce.order.domain.entity.OrderItemTea
import io.hhplus.ecommerce.order.domain.repository.OrderItemTeaRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Repository
class InMemoryOrderItemTeaRepository : OrderItemTeaRepository {
    private val storage = ConcurrentHashMap<Long, OrderItemTea>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val orderItemTea1 = OrderItemTea.create(
            orderItemId = 1L,
            productId = 1L,
            productName = "프리미엄 녹차",
            categoryName = "녹차",
            selectionOrder = 1,
            ratioPercent = 60,
            unitPrice = 5000
        ).copy(id = idGenerator.getAndIncrement())

        val orderItemTea2 = OrderItemTea.create(
            orderItemId = 1L,
            productId = 2L,
            productName = "얼 그레이 홍차",
            categoryName = "홍차",
            selectionOrder = 2,
            ratioPercent = 40,
            unitPrice = 4500
        ).copy(id = idGenerator.getAndIncrement())

        val orderItemTea3 = OrderItemTea.create(
            orderItemId = 2L,
            productId = 3L,
            productName = "캐모마일 허브차",
            categoryName = "허브차",
            selectionOrder = 1,
            ratioPercent = 100,
            unitPrice = 6000
        ).copy(id = idGenerator.getAndIncrement())

        storage[orderItemTea1.id] = orderItemTea1
        storage[orderItemTea2.id] = orderItemTea2
        storage[orderItemTea3.id] = orderItemTea3
    }

    override fun save(orderItemTea: OrderItemTea): OrderItemTea {
        simulateLatency()

        val savedEntity = if (orderItemTea.id == 0L) {
            orderItemTea.copy(id = idGenerator.getAndIncrement())
        } else {
            orderItemTea
        }
        storage[savedEntity.id] = savedEntity
        return savedEntity
    }

    override fun findById(id: Long): OrderItemTea? {
        simulateLatency()
        return storage[id]
    }

    override fun findByOrderItemId(orderItemId: Long): List<OrderItemTea> {
        simulateLatency()
        return storage.values.filter { it.orderItemId == orderItemId }
    }

    override fun deleteById(id: Long) {
        simulateLatency()
        storage.remove(id)
    }

    override fun deleteByOrderItemId(orderItemId: Long) {
        simulateLatency()
        storage.values.removeIf { it.orderItemId == orderItemId }
    }

    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    fun clear() {
        storage.clear()
        idGenerator.set(1)
        initializeSampleData()
    }

    private fun OrderItemTea.copy(
        id: Long = this.id,
        orderItemId: Long = this.orderItemId,
        productId: Long = this.productId,
        productName: String = this.productName,
        categoryName: String = this.categoryName,
        selectionOrder: Int = this.selectionOrder,
        ratioPercent: Int = this.ratioPercent,
        unitPrice: Int = this.unitPrice
    ): OrderItemTea {
        return OrderItemTea(id, orderItemId, productId, productName, categoryName, selectionOrder, ratioPercent, unitPrice)
    }
}