package io.hhplus.ecommerce.common.exception.order

import io.hhplus.ecommerce.common.exception.ErrorCode

/**
 * 주문 관련 에러 코드
 */
enum class OrderErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    ORDER_NOT_FOUND(
        code = "ORDER001",
        message = "주문을 찾을 수 없습니다. ID: {orderId}",
        httpStatus = 404
    ),

    ORDER_CANCELLATION_NOT_ALLOWED(
        code = "ORDER002",
        message = "취소할 수 없는 주문 상태입니다. 주문번호: {orderNumber}, 현재상태: {currentStatus}",
        httpStatus = 409
    ),

    INVALID_ORDER_STATUS(
        code = "ORDER003",
        message = "잘못된 주문 상태 변경입니다. 주문번호: {orderNumber}, 현재: {currentStatus}, 시도: {attemptedStatus}",
        httpStatus = 409
    );
}