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
        val orderItemTea1 = OrderItemTea(
            id = idGenerator.getAndIncrement(),
            orderItemId = 1L,
            productId = 1L,
            quantity = 2
        )

        val orderItemTea2 = OrderItemTea(
            id = idGenerator.getAndIncrement(),
            orderItemId = 1L,
            productId = 2L,
            quantity = 1
        )

        val orderItemTea3 = OrderItemTea(
            id = idGenerator.getAndIncrement(),
            orderItemId = 2L,
            productId = 3L,
            quantity = 3
        )

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
        quantity: Int = this.quantity
    ): OrderItemTea {
        return OrderItemTea(id, orderItemId, productId, quantity)
    }
}