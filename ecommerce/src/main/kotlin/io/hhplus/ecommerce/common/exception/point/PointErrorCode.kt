package io.hhplus.ecommerce.common.exception.point

import io.hhplus.ecommerce.common.exception.ErrorCode

/**
 * 포인트 관련 에러 코드
 */
enum class PointErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    INSUFFICIENT_POINT_BALANCE(
        code = "POINT001",
        message = "포인트 잔액이 부족합니다. 현재잔액: {currentBalance}, 사용시도금액: {useAmount}",
        httpStatus = 400
    ),

    MAX_POINT_BALANCE_EXCEEDED(
        code = "POINT002",
        message = "최대 포인트 잔액을 초과할 수 없습니다. 최대잔액: {maxBalance}, 현재잔액: {currentBalance}, 적립시도금액: {earnAmount}",
        httpStatus = 400
    ),

    INVALID_POINT_AMOUNT(
        code = "POINT003",
        message = "유효하지 않은 포인트 금액입니다: {amount}",
        httpStatus = 400
    ),

    POINT_NOT_FOUND(
        code = "POINT004",
        message = "사용자 포인트 정보를 찾을 수 없습니다. 사용자ID: {userId}",
        httpStatus = 404
    ),

    MINIMUM_USE_AMOUNT(
        code = "POINT005",
        message = "최소 사용 금액은 {minAmount}원입니다. 요청금액: {requestAmount}",
        httpStatus = 400
    ),

    POINT_PROCESSING_ERROR(
        code = "POINT006",
        message = "포인트 처리 중 오류가 발생했습니다: {reason}",
        httpStatus = 500
    ),

    INVALID_BALANCE(
        code = "POINT007",
        message = "잘못된 잔액입니다. 잔액은 0원 이상 {maxBalance}원 이하여야 합니다: {balance}",
        httpStatus = 400
    );
}
