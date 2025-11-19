package io.hhplus.ecommerce.order.domain.entity

import io.hhplus.ecommerce.order.exception.OrderException
import io.hhplus.ecommerce.order.domain.constant.OrderStatus

/**
 * Order 도메인 모델 (mutable)
 *
 * 역할:
 * - 주문의 핵심 비즈니스 로직 포함
 * - 상태 변경은 직접 필드 수정 (가변 구조)
 * - JPA 의존성 제거로 도메인 순수성 유지
 */
data class Order(
    val id: Long = 0,
    val orderNumber: String,
    val userId: Long,
    val totalAmount: Long,
    val discountAmount: Long = 0,
    val finalAmount: Long,
    val usedCouponId: Long? = null,
    var status: OrderStatus = OrderStatus.PENDING
) {


    /**
     * 주문 확정
     */
    fun confirm() {
        validateStatusTransition(OrderStatus.CONFIRMED)
        this.status = OrderStatus.CONFIRMED
    }

    /**
     * 주문 취소
     */
    fun cancel() {
        if (!canBeCancelled()) {
            throw OrderException.OrderCancellationNotAllowed(orderNumber, status)
        }
        this.status = OrderStatus.CANCELLED
    }

    /**
     * 주문 완료
     */
    fun complete() {
        validateStatusTransition(OrderStatus.COMPLETED)
        this.status = OrderStatus.COMPLETED
    }

    /**
     * 주문 실패
     */
    fun fail() {
        this.status = OrderStatus.FAILED
    }

    fun canBeCancelled(): Boolean = status.canBeCancelled()

    fun isPaid(): Boolean = status.isPaid()

    private fun validateStatusTransition(newStatus: OrderStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw OrderException.InvalidOrderStatus(orderNumber, status, newStatus)
        }
    }

    companion object {
        fun create(
            orderNumber: String,
            userId: Long,
            totalAmount: Long,
            discountAmount: Long = 0,
            usedCouponId: Long? = null
        ): Order {
            require(orderNumber.isNotBlank()) { "주문번호는 필수입니다" }
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(totalAmount > 0) { "총 금액은 0보다 커야 합니다" }
            require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다" }

            val finalAmount = totalAmount - discountAmount
            require(finalAmount >= 0) { "최종 금액은 0 이상이어야 합니다" }

            return Order(
                orderNumber = orderNumber,
                userId = userId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                usedCouponId = usedCouponId
            )
        }
    }
}