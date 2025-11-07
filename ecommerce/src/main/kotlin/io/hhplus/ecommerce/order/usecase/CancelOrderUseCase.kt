package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.domain.entity.Order
import org.springframework.stereotype.Component

/**
 * 주문 취소 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 주문 취소 비즈니스 플로우 수행
 * - 취소 권한 검증 및 연관 데이터 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 주문 취소 가능 상태 및 권한 검증
 * - 주문 취소 트랜잭션 관리
 * - 취소에 따른 재고 냀 결제 환불 후처리
 */
@Component
class CancelOrderUseCase(
    private val orderService: OrderService
) {

    /**
     * 지정된 주문을 취소하고 관련 후처리를 수행한다
     *
     * @param orderId 취소할 주문 ID
     * @param cancelledBy 취소를 요청하는 사용자 ID
     * @param reason 주문 취소 사유 (선택적)
     * @return 취소 처리가 완료된 주문 정보
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 취소 권한이 없는 경우
     * @throws RuntimeException 주문 취소 처리에 실패한 경우
     */
    fun execute(orderId: Long, cancelledBy: Long, reason: String?): Order {
        return orderService.cancelOrder(orderId, cancelledBy, reason)
    }
}