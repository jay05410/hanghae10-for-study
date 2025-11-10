package io.hhplus.ecommerce.point.domain.vo

import io.hhplus.ecommerce.common.exception.point.PointException

/**
 * 포인트 잔액 Value Object
 *
 * 비즈니스 규칙:
 * - 잔액은 0 이상이어야 함
 * - 최대 잔액은 10,000,000원 (천만원)
 * - 불변 객체로 안전한 연산 보장
 *
 * @property value 포인트 잔액 (원)
 */
@JvmInline
value class Balance private constructor(val value: Long) {
    init {
        if (value < 0 || value > MAX_BALANCE) {
            throw PointException.InvalidBalance(value, MAX_BALANCE)
        }
    }

    operator fun plus(amount: Long): Balance {
        val newValue = value + amount
        if (newValue > MAX_BALANCE) {
            throw PointException.MaxBalanceExceeded(MAX_BALANCE, value, amount)
        }
        return Balance(newValue)
    }

    operator fun minus(amount: Long): Balance {
        if (value < amount) {
            throw PointException.InsufficientBalance(value, amount)
        }
        return Balance(value - amount)
    }

    operator fun compareTo(other: Balance): Int = value.compareTo(other.value)
    operator fun compareTo(amount: Long): Int = value.compareTo(amount)

    fun canAfford(amount: Long): Boolean = value >= amount
    fun getFormattedBalance(): String = "${String.format("%,d", value)}원"

    companion object {
        const val MAX_BALANCE = 10_000_000L // 1천만원

        fun of(value: Long): Balance = Balance(value)
        fun zero(): Balance = Balance(0)
    }
}
