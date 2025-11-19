package io.hhplus.ecommerce.order.infra.persistence.repository

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.infra.persistence.entity.OrderJpaEntity
import org.springframework.data.jpa.repository.EntityGraph
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
 * N+1 문제 방지:
 * - EntityGraph: 페이지네이션 지원, 런타임 유연성 (주문 목록 조회)
 * - Fetch join: 단건 조회에 적합 (주문 상세 조회)
 */
interface OrderJpaRepository : JpaRepository<OrderJpaEntity, Long> {
    fun findByOrderNumber(orderNumber: String): OrderJpaEntity?
    fun findByUserId(userId: Long): List<OrderJpaEntity>


    fun findByUserIdAndStatus(userId: Long, status: OrderStatus): List<OrderJpaEntity>
    fun findByStatus(status: OrderStatus): List<OrderJpaEntity>
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<OrderJpaEntity>
    fun countByUserIdAndStatus(userId: Long, status: OrderStatus): Long

    /**
     * 사용자의 주문 목록을 orderItems와 함께 조회
     *
     * EntityGraph 사용 이유:
     * - 페이지네이션 지원 (향후 확장 가능)
     * - Fetch join은 페이지네이션과 함께 사용 시 메모리 페이징 발생
     */
    @EntityGraph(attributePaths = ["orderItems"])
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    fun findOrdersWithItemsByUserId(@Param("userId") userId: Long): List<OrderJpaEntity>

    /**
     * 주문 상세 조회 (단건)
     *
     * Fetch join 사용 이유:
     * - 단건 조회이므로 페이지네이션 불필요
     * - 명시적인 쿼리 제어 가능
     */
    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.orderItems WHERE o.id = :orderId")
    fun findOrderWithItemsById(@Param("orderId") orderId: Long): OrderJpaEntity?
}
