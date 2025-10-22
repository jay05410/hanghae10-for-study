package io.hhplus.tdd.common.exception.point

import io.hhplus.tdd.common.error.PointErrorCode
import io.hhplus.tdd.common.exception.BusinessException
import org.slf4j.event.Level

/**
 * 포인트 관련 예외 클래스
 *
 * 포인트 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 PointErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class PointException(
    errorCode: PointErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    // ===== 잔고 관련 =====

    /**
     * 잔고 부족 예외
     *
     * @param currentBalance 현재 잔고
     * @param requestAmount 요청 금액
     */
    class InsufficientBalance(currentBalance: Long, requestAmount: Long) : PointException(
        errorCode = PointErrorCode.INSUFFICIENT_BALANCE,
        message = PointErrorCode.INSUFFICIENT_BALANCE.withParams(
            "현재잔고" to currentBalance,
            "요청금액" to requestAmount
        ),
        data = mapOf(
            "currentBalance" to currentBalance,
            "requestAmount" to requestAmount
        )
    )

    // ===== 충전 관련 =====

    /**
     * 최소 충전 금액 미달 예외
     *
     * @param amount 요청 충전 금액
     */
    class MinimumChargeAmount(amount: Long) : PointException(
        errorCode = PointErrorCode.MINIMUM_CHARGE_AMOUNT,
        message = PointErrorCode.MINIMUM_CHARGE_AMOUNT.withParams("요청금액" to amount),
        data = mapOf("requestAmount" to amount)
    )

    /**
     * 최대 충전 금액 초과 예외
     *
     * @param amount 요청 충전 금액
     */
    class MaximumChargeAmount(amount: Long) : PointException(
        errorCode = PointErrorCode.MAXIMUM_CHARGE_AMOUNT,
        message = PointErrorCode.MAXIMUM_CHARGE_AMOUNT.withParams("요청금액" to amount),
        data = mapOf("requestAmount" to amount)
    )

    /**
     * 충전 단위 불일치 예외
     *
     * @param amount 요청 충전 금액
     */
    class InvalidChargeUnit(amount: Long) : PointException(
        errorCode = PointErrorCode.INVALID_CHARGE_UNIT,
        message = PointErrorCode.INVALID_CHARGE_UNIT.withParams("요청금액" to amount),
        data = mapOf("requestAmount" to amount)
    )

    // ===== 사용 관련 =====

    /**
     * 최소 사용 금액 미달 예외
     *
     * @param amount 요청 사용 금액
     */
    class MinimumUseAmount(amount: Long) : PointException(
        errorCode = PointErrorCode.MINIMUM_USE_AMOUNT,
        message = PointErrorCode.MINIMUM_USE_AMOUNT.withParams("요청금액" to amount),
        data = mapOf("requestAmount" to amount)
    )

    /**
     * 사용 단위 불일치 예외
     *
     * @param amount 요청 사용 금액
     */
    class InvalidUseUnit(amount: Long) : PointException(
        errorCode = PointErrorCode.INVALID_USE_UNIT,
        message = PointErrorCode.INVALID_USE_UNIT.withParams("요청금액" to amount),
        data = mapOf("requestAmount" to amount)
    )

    /**
     * 일일 사용 한도 초과 예외
     *
     * @param todayUsed 오늘 사용한 금액
     * @param requestAmount 요청 사용 금액
     */
    class DailyUseLimitExceeded(todayUsed: Long, requestAmount: Long) : PointException(
        errorCode = PointErrorCode.DAILY_USE_LIMIT_EXCEEDED,
        message = PointErrorCode.DAILY_USE_LIMIT_EXCEEDED.withParams(
            "오늘사용" to todayUsed,
            "요청금액" to requestAmount
        ),
        data = mapOf(
            "todayUsed" to todayUsed,
            "requestAmount" to requestAmount
        )
    )
}
