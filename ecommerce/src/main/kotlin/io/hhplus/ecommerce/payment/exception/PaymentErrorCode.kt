package io.hhplus.ecommerce.payment.exception

import io.hhplus.ecommerce.common.exception.ErrorCode

/**
 * 결제 관련 에러 코드
 */
enum class PaymentErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    INSUFFICIENT_BALANCE(
        code = "PAYMENT001",
        message = "잔액이 부족합니다. 현재잔액: {currentBalance}, 결제금액: {paymentAmount}",
        httpStatus = 409
    ),

    MINIMUM_CHARGE_AMOUNT(
        code = "PAYMENT002",
        message = "최소 충전 금액은 {minAmount}원입니다. 요청금액: {requestAmount}",
        httpStatus = 400
    ),

    MAXIMUM_CHARGE_AMOUNT(
        code = "PAYMENT003",
        message = "최대 충전 금액은 {maxAmount}원입니다. 요청금액: {requestAmount}",
        httpStatus = 400
    ),

    INVALID_CHARGE_UNIT(
        code = "PAYMENT004",
        message = "포인트 충전은 {unit}원 단위로만 가능합니다. 요청금액: {requestAmount}",
        httpStatus = 400
    ),

    PAYMENT_PROCESSING_ERROR(
        code = "PAYMENT005",
        message = "결제 처리 중 오류가 발생했습니다: {reason}",
        httpStatus = 402
    ),

    PAYMENT_CANCELLATION_NOT_ALLOWED(
        code = "PAYMENT006",
        message = "취소할 수 없는 결제 상태입니다. 결제번호: {paymentNumber}, 현재상태: {currentStatus}",
        httpStatus = 409
    ),

    INVALID_PAYMENT_STATUS(
        code = "PAYMENT007",
        message = "잘못된 결제 상태 변경입니다. 결제번호: {paymentNumber}, 현재: {currentStatus}, 시도: {attemptedStatus}",
        httpStatus = 409
    ),

    UNSUPPORTED_PAYMENT_METHOD(
        code = "PAYMENT008",
        message = "지원하지 않는 결제 수단입니다: {paymentMethod}",
        httpStatus = 400
    ),

    DUPLICATE_PAYMENT(
        code = "PAYMENT009",
        message = "중복 결제가 감지되었습니다",
        httpStatus = 409
    );
}