package io.hhplus.ecommerce.payment.domain.vo

import io.hhplus.ecommerce.common.exception.payment.PaymentException

/**
 * 포인트 충전 금액 Value Object
 *
 * 비즈니스 규칙:
 * - 최소 충전 금액: 1,000원
 * - 최대 충전 금액: 100,000원
 * - 충전 단위: 100원
 *
 * @property value 충전 금액
 */
@JvmInline
value class ChargeAmount private constructor(val value: Long) {

    companion object {
        private const val MIN = 1000L
        private const val MAX = 100000L
        private const val UNIT = 100L

        /**
         * ChargeAmount 생성
         *
         * @param amount 충전 금액
         * @return 검증된 ChargeAmount 객체
         * @throws PaymentException 비즈니스 규칙 위반 시
         */
        operator fun invoke(amount: Long): ChargeAmount {
            // 최소 금액 검증: 1,000원 미만은 시스템 수수료를 고려해 제한
            if (amount < MIN) {
                throw PaymentException.MinimumChargeAmount(amount, MIN)
            }

            // 최대 금액 검증: 100,000원 초과는 보안상 제한 (고액 결제는 별도 절차)
            if (amount > MAX) {
                throw PaymentException.MaximumChargeAmount(amount, MAX)
            }

            // 단위 검증: 100원 단위로만 충전 가능 (원화 최소 단위 고려)
            if (amount % UNIT != 0L) {
                throw PaymentException.InvalidChargeUnit(amount, UNIT)
            }

            return ChargeAmount(amount)
        }
    }
}