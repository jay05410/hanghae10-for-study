package io.hhplus.ecommerce.common.exception.delivery

import io.hhplus.ecommerce.common.exception.ErrorCode

/**
 * 배송 관련 에러 코드
 */
enum class DeliveryErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    DELIVERY_NOT_FOUND(
        code = "DELIVERY001",
        message = "배송 정보를 찾을 수 없습니다. ID: {deliveryId}",
        httpStatus = 404
    ),

    DELIVERY_NOT_FOUND_BY_ORDER(
        code = "DELIVERY002",
        message = "주문에 대한 배송 정보를 찾을 수 없습니다. 주문 ID: {orderId}",
        httpStatus = 404
    ),

    INVALID_DELIVERY_STATE_TRANSITION(
        code = "DELIVERY003",
        message = "잘못된 배송 상태 전환입니다. 현재: {currentState}, 시도: {attemptedState}",
        httpStatus = 409
    ),

    DELIVERY_ADDRESS_CHANGE_NOT_ALLOWED(
        code = "DELIVERY004",
        message = "배송지를 변경할 수 없는 상태입니다. 현재 배송 상태: {deliveryState}",
        httpStatus = 409
    ),

    RETURN_PERIOD_EXPIRED(
        code = "DELIVERY005",
        message = "반품 가능 기간이 지났습니다. 배송완료일: {deliveryDate}",
        httpStatus = 409
    ),

    TRACKING_INFO_REQUIRED(
        code = "DELIVERY006",
        message = "배송 발송을 위해 운송장 정보가 필요합니다.",
        httpStatus = 400
    );
}
