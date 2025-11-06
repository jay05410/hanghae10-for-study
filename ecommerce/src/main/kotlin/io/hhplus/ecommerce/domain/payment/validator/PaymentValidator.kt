package io.hhplus.ecommerce.domain.payment.validator

import io.hhplus.ecommerce.common.exception.payment.PaymentException

/**
 * 결제 검증 유틸리티
 *
 * 책임: DB 호출 없는 순수 검증 로직만 포함
 */
object PaymentValidator {

    /**
     * 잔고가 충분한지 검증
     *
     * @param currentBalance 현재 보유 잔고
     * @param paymentAmount 결제하려는 금액
     * @throws PaymentException.InsufficientBalance 잔고 부족 시
     */
    fun validateBalance(currentBalance: Long, paymentAmount: Long) {
        // 잔고 부족 검증: 결제 금액이 현재 잔고를 초과하는지 확인
        if (currentBalance < paymentAmount) {
            throw PaymentException.InsufficientBalance(currentBalance, paymentAmount)
        }
    }
}