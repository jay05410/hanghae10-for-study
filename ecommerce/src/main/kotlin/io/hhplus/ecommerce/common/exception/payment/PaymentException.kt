package io.hhplus.ecommerce.common.exception.payment

import io.hhplus.ecommerce.common.errorcode.PaymentErrorCode
import io.hhplus.ecommerce.common.exception.BusinessException
import io.hhplus.ecommerce.domain.payment.entity.PaymentStatus
import org.slf4j.event.Level

/**
 * 결제 관련 예외 클래스
 *
 * 결제 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 PaymentErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class PaymentException(
    errorCode: PaymentErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 잔액 부족 예외
     */
    class InsufficientBalance(currentBalance: Long, paymentAmount: Long) : PaymentException(
        errorCode = PaymentErrorCode.INSUFFICIENT_BALANCE,
        message = PaymentErrorCode.INSUFFICIENT_BALANCE.withParams(
            "currentBalance" to currentBalance,
            "paymentAmount" to paymentAmount
        ),
        data = mapOf(
            "currentBalance" to currentBalance,
            "paymentAmount" to paymentAmount
        )
    )

    /**
     * 최소 충전 금액 미달 예외
     */
    class MinimumChargeAmount(requestAmount: Long, minAmount: Long = 1000L) : PaymentException(
        errorCode = PaymentErrorCode.MINIMUM_CHARGE_AMOUNT,
        message = PaymentErrorCode.MINIMUM_CHARGE_AMOUNT.withParams(
            "minAmount" to minAmount,
            "requestAmount" to requestAmount
        ),
        data = mapOf(
            "minAmount" to minAmount,
            "requestAmount" to requestAmount
        )
    )

    /**
     * 최대 충전 금액 초과 예외
     */
    class MaximumChargeAmount(requestAmount: Long, maxAmount: Long = 100000L) : PaymentException(
        errorCode = PaymentErrorCode.MAXIMUM_CHARGE_AMOUNT,
        message = PaymentErrorCode.MAXIMUM_CHARGE_AMOUNT.withParams(
            "maxAmount" to maxAmount,
            "requestAmount" to requestAmount
        ),
        data = mapOf(
            "maxAmount" to maxAmount,
            "requestAmount" to requestAmount
        )
    )

    /**
     * 충전 단위 불일치 예외
     */
    class InvalidChargeUnit(requestAmount: Long, unit: Long = 100L) : PaymentException(
        errorCode = PaymentErrorCode.INVALID_CHARGE_UNIT,
        message = PaymentErrorCode.INVALID_CHARGE_UNIT.withParams(
            "unit" to unit,
            "requestAmount" to requestAmount
        ),
        data = mapOf(
            "unit" to unit,
            "requestAmount" to requestAmount
        )
    )

    /**
     * 결제 처리 오류 예외
     */
    class PaymentProcessingError(reason: String) : PaymentException(
        errorCode = PaymentErrorCode.PAYMENT_PROCESSING_ERROR,
        message = PaymentErrorCode.PAYMENT_PROCESSING_ERROR.withParams("reason" to reason),
        data = mapOf("reason" to reason)
    )

    /**
     * 결제 취소 불가 예외
     */
    class PaymentCancellationNotAllowed(paymentNumber: String, currentStatus: PaymentStatus) : PaymentException(
        errorCode = PaymentErrorCode.PAYMENT_CANCELLATION_NOT_ALLOWED,
        message = PaymentErrorCode.PAYMENT_CANCELLATION_NOT_ALLOWED.withParams(
            "paymentNumber" to paymentNumber,
            "currentStatus" to currentStatus
        ),
        data = mapOf(
            "paymentNumber" to paymentNumber,
            "currentStatus" to currentStatus
        )
    )

    /**
     * 잘못된 결제 상태 변경 예외
     */
    class InvalidPaymentStatus(paymentNumber: String, currentStatus: PaymentStatus, attemptedStatus: PaymentStatus) : PaymentException(
        errorCode = PaymentErrorCode.INVALID_PAYMENT_STATUS,
        message = PaymentErrorCode.INVALID_PAYMENT_STATUS.withParams(
            "paymentNumber" to paymentNumber,
            "currentStatus" to currentStatus,
            "attemptedStatus" to attemptedStatus
        ),
        data = mapOf(
            "paymentNumber" to paymentNumber,
            "currentStatus" to currentStatus,
            "attemptedStatus" to attemptedStatus
        )
    )
}