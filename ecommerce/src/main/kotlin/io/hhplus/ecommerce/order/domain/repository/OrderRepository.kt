package io.hhplus.ecommerce.order.domain.repository

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.entity.Order
import java.time.LocalDateTime

/**
 * Order Repository Interface - 도메인 계층
 *
 * 책임:
 * - Order 도메인의 영속성 인터페이스 정의
 * - 도메인에 필요한 데이터 접근 메서드 선언
 * - 구현체는 infra 계층에서 담당
 */
interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findByOrderNumber(orderNumber: String): Order?
    fun findByUserId(userId: Long): List<Order>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Order>
    fun findByUserIdAndStatus(userId: Long, status: OrderStatus): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Order>
    fun countByUserIdAndStatus(userId: Long, status: OrderStatus): Long

    /**
     * 사용자에게 노출되는 주문만 조회 (PENDING_PAYMENT, EXPIRED 제외)
     */
    fun findVisibleOrdersByUserId(userId: Long): List<Order>

    /**
     * 결제 완료된 주문 기간별 조회 (통계용)
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 결제 완료된 주문 목록 (PENDING, CONFIRMED, COMPLETED)
     */
    fun findPaidOrdersBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Order>
}