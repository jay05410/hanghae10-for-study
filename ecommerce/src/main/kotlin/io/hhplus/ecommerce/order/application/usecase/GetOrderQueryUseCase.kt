package io.hhplus.ecommerce.order.application.usecase

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 조회 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 주문 관련 다양한 조회 작업 통합 처리
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 주문 조회 사용 사례 통합 처리
 * - 읽기 전용 작업 처리
 */
@Component
class GetOrderQueryUseCase(
    private val orderDomainService: OrderDomainService
) {

    /**
     * 주문 ID로 특정 주문 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 정보 (존재하지 않으면 null)
     */
    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): Order? {
        return orderDomainService.getOrder(orderId)
    }

    /**
     * 주문 ID로 주문과 주문 아이템을 함께 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 정보와 주문 아이템 목록 Pair (존재하지 않으면 null)
     */
    @Transactional(readOnly = true)
    fun getOrderWithItems(orderId: Long): Pair<Order, List<OrderItem>>? {
        return orderDomainService.getOrderWithItems(orderId)
    }

    /**
     * 사용자가 진행한 모든 주문 목록 조회
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 주문 목록 (모든 상태 포함)
     */
    @Transactional(readOnly = true)
    fun getOrdersByUser(userId: Long): List<Order> {
        return orderDomainService.getOrdersByUser(userId)
    }

    /**
     * 사용자에게 노출되는 주문만 조회 (PENDING_PAYMENT, EXPIRED 제외)
     *
     * 사용자가 "내 주문 목록"에서 보는 주문:
     * - PENDING: 결제 완료, 처리 대기
     * - CONFIRMED: 주문 확정
     * - COMPLETED: 배송 완료
     * - CANCELLED: 취소됨
     * - FAILED: 결제 실패
     *
     * 노출되지 않는 주문:
     * - PENDING_PAYMENT: 결제 진행 중 (체크아웃 시작 후 결제 미완료)
     * - EXPIRED: 결제 시간 초과
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자에게 보이는 주문 목록
     */
    @Transactional(readOnly = true)
    fun getVisibleOrdersByUser(userId: Long): List<Order> {
        return orderDomainService.getVisibleOrdersByUser(userId)
    }

    /**
     * 사용자가 진행한 모든 주문과 주문 아이템을 함께 조회
     *
     * @param userId 인증된 사용자 ID
     * @return 주문 정보와 주문 아이템 목록의 Map
     */
    @Transactional(readOnly = true)
    fun getOrdersWithItemsByUser(userId: Long): Map<Order, List<OrderItem>> {
        return orderDomainService.getOrdersWithItemsByUser(userId)
    }

    /**
     * 사용자에게 노출되는 주문과 주문 아이템을 함께 조회 (PENDING_PAYMENT, EXPIRED 제외)
     *
     * 사용자가 "내 주문 목록"에서 보는 주문 (아이템 포함)
     *
     * @param userId 인증된 사용자 ID
     * @return 주문 정보와 주문 아이템 목록의 Map
     */
    @Transactional(readOnly = true)
    fun getVisibleOrdersWithItemsByUser(userId: Long): Map<Order, List<OrderItem>> {
        return orderDomainService.getVisibleOrdersWithItemsByUser(userId)
    }
}
