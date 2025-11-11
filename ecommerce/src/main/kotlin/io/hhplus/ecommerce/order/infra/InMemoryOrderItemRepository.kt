package io.hhplus.ecommerce.order.infra

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Repository
class InMemoryOrderItemRepository : OrderItemRepository {
    private val orderItems = ConcurrentHashMap<Long, OrderItem>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val sampleOrder = Order(
            id = 1L,
            orderNumber = "ORDER-001-20241107",
            userId = 1L,
            totalAmount = 45000L,
            discountAmount = 5000L,
            finalAmount = 40000L,
            usedCouponId = 1L,
            status = OrderStatus.COMPLETED
        )

        // Sample OrderItem 1 - 제주 유기농 녹차, 주간 티 박스
        val orderItem1 = OrderItem(
            id = idGenerator.getAndIncrement(),
            order = sampleOrder,
            productId = 1L, // 제주 유기농 녹차
            boxTypeId = 1L, // 주간 티 박스
            quantity = 2,
            unitPrice = 15000L,
            totalPrice = 30000L
        )

        // Sample OrderItem 2 - 전통 우롱차, 월간 티 박스
        val orderItem2 = OrderItem(
            id = idGenerator.getAndIncrement(),
            order = sampleOrder,
            productId = 2L, // 전통 우롱차
            boxTypeId = 2L, // 월간 티 박스
            quantity = 1,
            unitPrice = 20000L,
            totalPrice = 20000L
        )

        orderItems[orderItem1.id] = orderItem1
        orderItems[orderItem2.id] = orderItem2
    }

    override fun save(orderItem: OrderItem): OrderItem {
        simulateLatency()

        val savedOrderItem = if (orderItem.id == 0L) {
            OrderItem(
                id = idGenerator.getAndIncrement(),
                order = orderItem.order,
                productId = orderItem.productId,
                boxTypeId = orderItem.boxTypeId,
                quantity = orderItem.quantity,
                unitPrice = orderItem.unitPrice,
                totalPrice = orderItem.totalPrice
            )
        } else {
            orderItem
        }

        orderItems[savedOrderItem.id] = savedOrderItem
        return savedOrderItem
    }

    override fun findById(id: Long): OrderItem? {
        simulateLatency()
        return orderItems[id]
    }

    override fun findByOrderId(orderId: Long): List<OrderItem> {
        simulateLatency()
        return orderItems.values.filter { it.order.id == orderId }
    }

    override fun findByOrderIdAndProductId(orderId: Long, productId: Long): OrderItem? {
        simulateLatency()
        return orderItems.values.find { it.order.id == orderId && it.productId == productId }
    }

    override fun findByOrderIdAndProductIdAndBoxTypeId(orderId: Long, productId: Long, boxTypeId: Long): OrderItem? {
        simulateLatency()
        return orderItems.values.find {
            it.order.id == orderId &&
            it.productId == productId &&
            it.boxTypeId == boxTypeId
        }
    }

    override fun findByProductId(productId: Long): List<OrderItem> {
        simulateLatency()
        return orderItems.values.filter { it.productId == productId }
    }

    override fun deleteById(id: Long) {
        simulateLatency()
        orderItems.remove(id)
    }

    override fun deleteByOrderId(orderId: Long) {
        simulateLatency()
        orderItems.values.removeAll { it.order.id == orderId }
    }

    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    fun clear() {
        orderItems.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}