package io.hhplus.ecommerce.order.infra.persistence.repository

import io.hhplus.ecommerce.order.infra.persistence.entity.OrderItemJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * OrderItem JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 통한 자동 쿼리 메서드 생성
 * - JPA 엔티티 기반 데이터 접근
 */
interface OrderItemJpaRepository : JpaRepository<OrderItemJpaEntity, Long> {
    @Query("SELECT oi FROM OrderItemJpaEntity oi WHERE oi.orderId = :orderId")
    fun findByOrderId(@Param("orderId") orderId: Long): List<OrderItemJpaEntity>

    @Query("SELECT oi FROM OrderItemJpaEntity oi WHERE oi.orderId = :orderId AND oi.productId = :productId")
    fun findByOrderIdAndProductId(@Param("orderId") orderId: Long, @Param("productId") productId: Long): OrderItemJpaEntity?

    fun findByProductId(productId: Long): List<OrderItemJpaEntity>

    @Modifying
    @Query("DELETE FROM OrderItemJpaEntity oi WHERE oi.orderId = :orderId")
    fun deleteByOrderId(@Param("orderId") orderId: Long)
}
