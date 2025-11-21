package io.hhplus.ecommerce.order.exception

import io.hhplus.ecommerce.common.exception.BusinessException
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import org.slf4j.event.Level

/**
 * 주문 관련 예외 클래스
 *
 * 주문 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 OrderErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class OrderException(
    errorCode: OrderErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 주문을 찾을 수 없음 예외
     */
    class OrderNotFound(orderId: Long) : OrderException(
        errorCode = OrderErrorCode.ORDER_NOT_FOUND,
        message = OrderErrorCode.ORDER_NOT_FOUND.withParams("orderId" to orderId),
        data = mapOf("orderId" to orderId)
    )

    /**
     * 주문 취소 불가 예외
     */
    class OrderCancellationNotAllowed(orderNumber: String, currentStatus: OrderStatus) : OrderException(
        errorCode = OrderErrorCode.ORDER_CANCELLATION_NOT_ALLOWED,
        message = OrderErrorCode.ORDER_CANCELLATION_NOT_ALLOWED.withParams(
            "orderNumber" to orderNumber,
            "currentStatus" to currentStatus
        ),
        data = mapOf(
            "orderNumber" to orderNumber,
            "currentStatus" to currentStatus
        )
    )

    /**
     * 잘못된 주문 상태 변경 예외
     */
    class InvalidOrderStatus(orderNumber: String, currentStatus: OrderStatus, attemptedStatus: OrderStatus) : OrderException(
        errorCode = OrderErrorCode.INVALID_ORDER_STATUS,
        message = OrderErrorCode.INVALID_ORDER_STATUS.withParams(
            "orderNumber" to orderNumber,
            "currentStatus" to currentStatus,
            "attemptedStatus" to attemptedStatus
        ),
        data = mapOf(
            "orderNumber" to orderNumber,
            "currentStatus" to currentStatus,
            "attemptedStatus" to attemptedStatus
        )
    )

    /**
     * 이미 주문 대기열에 등록된 사용자 예외
     */
    class AlreadyInOrderQueue(userId: Long) : OrderException(
        errorCode = OrderErrorCode.ALREADY_IN_ORDER_QUEUE,
        message = OrderErrorCode.ALREADY_IN_ORDER_QUEUE.withParams("userId" to userId),
        data = mapOf("userId" to userId)
    )
}