package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.domain.entity.Order
import org.springframework.stereotype.Component

/**
 * 주문 확정 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 주문 확정 비즈니스 플로우 수행
 * - 확정 권한 검증 및 주문 상태 변경
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 주문 확정 가능 상태 및 권한 검증
 * - 주문 확정 트랜잭션 관리
 * - 확정에 따른 대기 중 결제 및 재고 처리
 */
@Component
class ConfirmOrderUseCase(
    private val orderService: OrderService
) {

    /**
     * 지정된 주문을 확정하고 최종 처리를 수행한다
     *
     * @param orderId 확정할 주문 ID
     * @param confirmedBy 확정을 수행하는 사용자 ID
     * @return 확정 처리가 완료된 주문 정보
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 확정 권한이 없는 경우
     * @throws RuntimeException 주문 확정 처리에 실패한 경우
     */
    fun execute(orderId: Long, confirmedBy: Long): Order {
        return orderService.confirmOrder(orderId, confirmedBy)
    }
}