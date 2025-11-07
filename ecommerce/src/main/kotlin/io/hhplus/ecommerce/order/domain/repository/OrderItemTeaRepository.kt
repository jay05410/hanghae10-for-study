package io.hhplus.ecommerce.order.domain.repository

import io.hhplus.ecommerce.order.domain.entity.OrderItemTea

interface OrderItemTeaRepository {
    fun save(orderItemTea: OrderItemTea): OrderItemTea
    fun findById(id: Long): OrderItemTea?
    fun findByOrderItemId(orderItemId: Long): List<OrderItemTea>
    fun deleteById(id: Long)
    fun deleteByOrderItemId(orderItemId: Long)
}