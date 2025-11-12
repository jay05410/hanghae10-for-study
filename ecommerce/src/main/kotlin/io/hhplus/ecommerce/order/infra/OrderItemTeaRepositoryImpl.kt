package io.hhplus.ecommerce.order.infra

import io.hhplus.ecommerce.order.domain.entity.OrderItemTea
import io.hhplus.ecommerce.order.domain.repository.OrderItemTeaRepository
import io.hhplus.ecommerce.order.infra.persistence.repository.OrderItemTeaJpaRepository
import org.springframework.stereotype.Repository

/**
 * OrderItemTea Repository JPA 구현체
 */
@Repository
class OrderItemTeaRepositoryImpl(
    private val jpaRepository: OrderItemTeaJpaRepository
) : OrderItemTeaRepository {

    override fun save(orderItemTea: OrderItemTea): OrderItemTea =
        jpaRepository.save(orderItemTea)

    override fun findById(id: Long): OrderItemTea? =
        jpaRepository.findById(id).orElse(null)

    override fun findByOrderItemId(orderItemId: Long): List<OrderItemTea> =
        jpaRepository.findByOrderItemId(orderItemId)

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteByOrderItemId(orderItemId: Long) {
        jpaRepository.deleteByOrderItemId(orderItemId)
    }
}