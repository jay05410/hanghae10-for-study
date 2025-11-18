package io.hhplus.ecommerce.order.infra.persistence.repository

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.infra.persistence.entity.OrderJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * Order JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 통한 자동 쿼리 메서드 생성
 * - JPA 엔티티 기반 데이터 접근
 */
interface OrderJpaRepository : JpaRepository<OrderJpaEntity, Long> {
    fun findByOrderNumber(orderNumber: String): OrderJpaEntity?
    fun findByUserId(userId: Long): List<OrderJpaEntity>


    fun findByUserIdAndStatus(userId: Long, status: OrderStatus): List<OrderJpaEntity>
    fun findByStatus(status: OrderStatus): List<OrderJpaEntity>
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<OrderJpaEntity>
    fun countByUserIdAndStatus(userId: Long, status: OrderStatus): Long

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    fun findOrdersWithItemsByUserId(@Param("userId") userId: Long): List<OrderJpaEntity>

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.orderItems WHERE o.id = :orderId")
    fun findOrderWithItemsById(@Param("orderId") orderId: Long): OrderJpaEntity?
}
