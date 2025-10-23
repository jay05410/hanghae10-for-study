package io.hhplus.tdd.point.vo

import io.hhplus.tdd.common.exception.point.PointException

/**
 * 포인트 충전 금액 Value Object
 *
 * 비즈니스 규칙:
 * - 최소 충전 금액: 1,000원
 * - 최대 충전 금액: 1,000,000원
 * - 충전 단위: 100원
 *
 * @property value 충전 금액
 */
@JvmInline
value class ChargeAmount private constructor(val value: Long) {

    companion object {
        private const val MIN = 1000L
        private const val MAX = 1000000L
        private const val UNIT = 100L

        /**
         * ChargeAmount 생성
         *
         * @param amount 충전 금액
         * @return 검증된 ChargeAmount 객체
         * @throws PointException 비즈니스 규칙 위반 시
         */
        operator fun invoke(amount: Long): ChargeAmount {
            // 최소 금액 검증
            if (amount < MIN) {
                throw PointException.MinimumChargeAmount(amount)
            }

            // 최대 금액 검증
            if (amount > MAX) {
                throw PointException.MaximumChargeAmount(amount)
            }

            // 단위 검증
            if (amount % UNIT != 0L) {
                throw PointException.InvalidChargeUnit(amount)
            }

            return ChargeAmount(amount)
        }
    }
}