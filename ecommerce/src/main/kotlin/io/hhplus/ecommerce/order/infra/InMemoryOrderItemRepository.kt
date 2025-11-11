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

        // Sample OrderItem 1 - 3일 패키지
        val orderItem1 = OrderItem(
            id = idGenerator.getAndIncrement(),
            orderId = sampleOrder.id,
            packageTypeId = 1L, // THREE_DAYS 패키지
            packageTypeName = "3일 패키지",
            packageTypeDays = 3,
            dailyServing = 1,
            totalQuantity = 30.0,
            giftWrap = false,
            giftMessage = null,
            quantity = 1,
            containerPrice = 5000,
            teaPrice = 15000,
            giftWrapPrice = 0,
            totalPrice = 20000
        )

        // Sample OrderItem 2 - 7일 패키지
        val orderItem2 = OrderItem(
            id = idGenerator.getAndIncrement(),
            orderId = sampleOrder.id,
            packageTypeId = 2L, // SEVEN_DAYS 패키지
            packageTypeName = "7일 패키지",
            packageTypeDays = 7,
            dailyServing = 1,
            totalQuantity = 70.0,
            giftWrap = true,
            giftMessage = "건강한 하루 되세요",
            quantity = 1,
            containerPrice = 8000,
            teaPrice = 25000,
            giftWrapPrice = 2000,
            totalPrice = 35000
        )

        orderItems[orderItem1.id] = orderItem1
        orderItems[orderItem2.id] = orderItem2
    }

    override fun save(orderItem: OrderItem): OrderItem {
        simulateLatency()

        val savedOrderItem = if (orderItem.id == 0L) {
            OrderItem(
                id = idGenerator.getAndIncrement(),
                orderId = orderItem.orderId,
                packageTypeId = orderItem.packageTypeId,
                packageTypeName = orderItem.packageTypeName,
                packageTypeDays = orderItem.packageTypeDays,
                dailyServing = orderItem.dailyServing,
                totalQuantity = orderItem.totalQuantity,
                giftWrap = orderItem.giftWrap,
                giftMessage = orderItem.giftMessage,
                quantity = orderItem.quantity,
                containerPrice = orderItem.containerPrice,
                teaPrice = orderItem.teaPrice,
                giftWrapPrice = orderItem.giftWrapPrice,
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
        return orderItems.values.filter { it.orderId == orderId }
    }

    override fun findByOrderIdAndProductId(orderId: Long, productId: Long): OrderItem? {
        simulateLatency()
        return orderItems.values.find { it.orderId == orderId && it.packageTypeId == productId }
    }

    override fun findByOrderIdAndProductIdAndBoxTypeId(orderId: Long, productId: Long, boxTypeId: Long): OrderItem? {
        simulateLatency()
        return orderItems.values.find {
            it.orderId == orderId &&
            it.packageTypeId == boxTypeId
        }
    }

    override fun findByProductId(productId: Long): List<OrderItem> {
        simulateLatency()
        return orderItems.values.filter { it.packageTypeId == productId }
    }

    override fun deleteById(id: Long) {
        simulateLatency()
        orderItems.remove(id)
    }

    override fun deleteByOrderId(orderId: Long) {
        simulateLatency()
        orderItems.values.removeAll { it.orderId == orderId }
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