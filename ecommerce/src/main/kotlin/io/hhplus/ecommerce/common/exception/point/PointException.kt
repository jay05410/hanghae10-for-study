package io.hhplus.ecommerce.common.exception.point

import io.hhplus.ecommerce.common.exception.BusinessException
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

    /**
     * 포인트 잔액 부족 예외
     */
    class InsufficientBalance(currentBalance: Long, useAmount: Long) : PointException(
        errorCode = PointErrorCode.INSUFFICIENT_POINT_BALANCE,
        message = PointErrorCode.INSUFFICIENT_POINT_BALANCE.withParams(
            "currentBalance" to currentBalance,
            "useAmount" to useAmount
        ),
        data = mapOf(
            "currentBalance" to currentBalance,
            "useAmount" to useAmount
        )
    )

    /**
     * 최대 포인트 잔액 초과 예외
     */
    class MaxBalanceExceeded(maxBalance: Long, currentBalance: Long, earnAmount: Long) : PointException(
        errorCode = PointErrorCode.MAX_POINT_BALANCE_EXCEEDED,
        message = PointErrorCode.MAX_POINT_BALANCE_EXCEEDED.withParams(
            "maxBalance" to maxBalance,
            "currentBalance" to currentBalance,
            "earnAmount" to earnAmount
        ),
        data = mapOf(
            "maxBalance" to maxBalance,
            "currentBalance" to currentBalance,
            "earnAmount" to earnAmount
        )
    )

    /**
     * 유효하지 않은 포인트 금액 예외
     */
    class InvalidAmount(amount: Long) : PointException(
        errorCode = PointErrorCode.INVALID_POINT_AMOUNT,
        message = PointErrorCode.INVALID_POINT_AMOUNT.withParams("amount" to amount),
        data = mapOf("amount" to amount)
    )

    /**
     * 포인트 정보 없음 예외
     */
    class PointNotFound(userId: Long) : PointException(
        errorCode = PointErrorCode.POINT_NOT_FOUND,
        message = PointErrorCode.POINT_NOT_FOUND.withParams("userId" to userId),
        data = mapOf("userId" to userId)
    )

    /**
     * 최소 사용 금액 미달 예외
     */
    class MinimumUseAmount(requestAmount: Long, minAmount: Long = 100L) : PointException(
        errorCode = PointErrorCode.MINIMUM_USE_AMOUNT,
        message = PointErrorCode.MINIMUM_USE_AMOUNT.withParams(
            "minAmount" to minAmount,
            "requestAmount" to requestAmount
        ),
        data = mapOf(
            "minAmount" to minAmount,
            "requestAmount" to requestAmount
        )
    )

    /**
     * 포인트 처리 오류 예외
     */
    class ProcessingError(reason: String) : PointException(
        errorCode = PointErrorCode.POINT_PROCESSING_ERROR,
        message = PointErrorCode.POINT_PROCESSING_ERROR.withParams("reason" to reason),
        data = mapOf("reason" to reason),
        logLevel = Level.ERROR
    )

    /**
     * 잘못된 잔액 예외
     */
    class InvalidBalance(balance: Long, maxBalance: Long = 10_000_000L) : PointException(
        errorCode = PointErrorCode.INVALID_BALANCE,
        message = PointErrorCode.INVALID_BALANCE.withParams(
            "balance" to balance,
            "maxBalance" to maxBalance
        ),
        data = mapOf(
            "balance" to balance,
            "maxBalance" to maxBalance
        )
    )
}
