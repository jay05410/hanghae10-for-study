package io.hhplus.ecommerce.order.infra.persistence.repository

import io.hhplus.ecommerce.order.infra.persistence.entity.OrderItemJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * OrderItem JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 통한 자동 쿼리 메서드 생성
 * - JPA 엔티티 기반 데이터 접근
 */
interface OrderItemJpaRepository : JpaRepository<OrderItemJpaEntity, Long> {
    fun findByOrderId(orderId: Long): List<OrderItemJpaEntity>
    fun findByOrderIdAndPackageTypeId(orderId: Long, productId: Long): OrderItemJpaEntity?
    fun findByPackageTypeId(productId: Long): List<OrderItemJpaEntity>
    fun deleteByOrderId(orderId: Long)
}
