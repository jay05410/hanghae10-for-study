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
 *
 * 주의:
 * - OrderItem은 별도 Repository를 통해 조회해야 합니다.
 */
interface OrderJpaRepository : JpaRepository<OrderJpaEntity, Long> {
    fun findByOrderNumber(orderNumber: String): OrderJpaEntity?
    fun findByUserId(userId: Long): List<OrderJpaEntity>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<OrderJpaEntity>


    fun findByUserIdAndStatus(userId: Long, status: OrderStatus): List<OrderJpaEntity>
    fun findByStatus(status: OrderStatus): List<OrderJpaEntity>
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<OrderJpaEntity>
    fun countByUserIdAndStatus(userId: Long, status: OrderStatus): Long

    /**
     * 사용자에게 노출되는 주문만 조회 (PENDING_PAYMENT, EXPIRED 제외)
     */
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE o.userId = :userId
        AND o.status NOT IN (:excludedStatuses)
        ORDER BY o.createdAt DESC
    """)
    fun findVisibleOrdersByUserId(
        @Param("userId") userId: Long,
        @Param("excludedStatuses") excludedStatuses: List<OrderStatus>
    ): List<OrderJpaEntity>

    /**
     * 결제 완료된 주문 기간별 조회 (통계용)
     */
    @Query("""
        SELECT o FROM OrderJpaEntity o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        AND o.status IN (:paidStatuses)
    """)
    fun findPaidOrdersBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("paidStatuses") paidStatuses: List<OrderStatus>
    ): List<OrderJpaEntity>
}
