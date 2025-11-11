package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.domain.entity.Order
import org.springframework.stereotype.Component

/**
 * 주문 조회 통합 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 주문 관련 다양한 조회 작업 통합 처리
 * - 사용자별 주문 정보 조회 및 비즈니스 로직 수행
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 주문 조회 사용 사례 통합 처리
 * - 주문 데이터 반환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetOrderQueryUseCase(
    private val orderService: OrderService
) {

    /**
     * 주문 ID로 특정 주문을 조회한다
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 정보 (존재하지 않으면 null 반환)
     */
    fun getOrder(orderId: Long): Order? {
        return orderService.getOrder(orderId)
    }

    /**
     * 사용자가 진행한 모든 주문 목록을 조회한다
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 주문 목록 (모든 상태 포함)
     */
    fun getOrdersByUser(userId: Long): List<Order> {
        return orderService.getOrdersByUser(userId)
    }
}