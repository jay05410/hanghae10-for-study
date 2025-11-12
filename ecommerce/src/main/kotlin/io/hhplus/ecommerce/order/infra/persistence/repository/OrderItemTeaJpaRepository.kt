package io.hhplus.ecommerce.order.infra.persistence.repository

import io.hhplus.ecommerce.order.domain.entity.OrderItemTea
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface OrderItemTeaJpaRepository : JpaRepository<OrderItemTea, Long> {

    fun findByOrderItemId(orderItemId: Long): List<OrderItemTea>

    @Modifying
    @Query("DELETE FROM OrderItemTea o WHERE o.id = :id")
    override fun deleteById(id: Long)

    @Modifying
    @Query("DELETE FROM OrderItemTea o WHERE o.orderItemId = :orderItemId")
    fun deleteByOrderItemId(orderItemId: Long)
}