package io.hhplus.ecommerce.order.domain.repository

import io.hhplus.ecommerce.order.domain.entity.OrderItem

interface OrderItemRepository {
    fun save(orderItem: OrderItem): OrderItem
    fun findById(id: Long): OrderItem?
    fun findByOrderId(orderId: Long): List<OrderItem>
    fun findByOrderIdIn(orderIds: List<Long>): List<OrderItem>
    fun findByOrderIdAndProductId(orderId: Long, productId: Long): OrderItem?
    fun findByProductId(productId: Long): List<OrderItem>
    fun deleteById(id: Long)
    fun deleteByOrderId(orderId: Long)
}