package io.hhplus.ecommerce.order.domain.validator

import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.common.exception.cart.CartException
import io.hhplus.ecommerce.common.exception.order.OrderException
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.entity.Order

/**
 * 주문 검증 유틸리티
 *
 * 책임: DB 호출 없는 순수 검증 로직만 포함
 */
object OrderValidator {

    /**
     * 빈 장바구니 검증
     *
     * @param cartItems 장바구니 아이템들
     * @throws io.hhplus.ecommerce.common.exception.cart.CartException.EmptyCart 장바구니가 비어있을 시
     */
    fun validateCartNotEmpty(cartItems: List<CartItem>) {
        if (cartItems.isEmpty()) {
            throw CartException.EmptyCart()
        }
    }

    /**
     * 주문 취소 가능 여부 검증
     *
     * @param order 주문
     * @throws io.hhplus.ecommerce.common.exception.order.OrderException.OrderCancellationNotAllowed 취소 불가능한 상태일 시
     */
    fun validateOrderCancellable(order: Order) {
        if (!order.canBeCancelled()) {
            throw OrderException.OrderCancellationNotAllowed(order.orderNumber, order.status)
        }
    }

    /**
     * 주문 상태 전이 가능 여부 검증
     *
     * @param currentStatus 현재 상태
     * @param newStatus 새 상태
     * @param orderNumber 주문 번호
     * @throws OrderException.InvalidOrderStatus 잘못된 상태 전이 시
     */
    fun validateStatusTransition(currentStatus: OrderStatus, newStatus: OrderStatus, orderNumber: String) {
        val validTransitions = when (currentStatus) {
            OrderStatus.PENDING -> listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.FAILED)
            OrderStatus.CONFIRMED -> listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
            OrderStatus.COMPLETED -> emptyList()
            OrderStatus.CANCELLED -> emptyList()
            OrderStatus.FAILED -> emptyList()
        }

        if (newStatus !in validTransitions) {
            throw OrderException.InvalidOrderStatus(orderNumber, currentStatus, newStatus)
        }
    }

    /**
     * 주문 금액 유효성 검증
     *
     * @param totalAmount 총 금액
     * @param discountAmount 할인 금액
     * @throws IllegalArgumentException 유효하지 않은 금액일 시
     */
    fun validateOrderAmounts(totalAmount: Long, discountAmount: Long) {
        require(totalAmount > 0) { "총 금액은 0보다 커야 합니다: $totalAmount" }
        require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다: $discountAmount" }
        require(totalAmount >= discountAmount) { "할인 금액이 총 금액을 초과할 수 없습니다" }
    }
}