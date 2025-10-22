package io.hhplus.tdd.point.vo

import io.hhplus.tdd.common.exception.point.PointException

/**
 * 포인트 사용 금액 Value Object
 *
 * 비즈니스 규칙:
 * - 최소 사용 금액: 100원
 * - 사용 단위: 100원
 *
 * @property value 사용 금액
 */
@JvmInline
value class UseAmount private constructor(val value: Long) {

    companion object {
        private const val MIN = 100L
        private const val UNIT = 100L

        /**
         * UseAmount 생성
         *
         * @param amount 사용 금액
         * @return 검증된 UseAmount 객체
         * @throws PointException 비즈니스 규칙 위반 시
         */
        operator fun invoke(amount: Long): UseAmount {
            // 최소 금액 검증
            if (amount < MIN) {
                throw PointException.MinimumUseAmount(amount)
            }

            // 단위 검증
            if (amount % UNIT != 0L) {
                throw PointException.InvalidUseUnit(amount)
            }

            return UseAmount(amount)
        }
    }
}