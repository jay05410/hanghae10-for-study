package io.hhplus.ecommerce.user.domain.vo

@JvmInline
value class Balance private constructor(val value: Long) {
    init {
        require(value >= 0) { "잔액은 0 이상이어야 합니다: $value" }
        require(value <= MAX_BALANCE) { "잔액은 ${MAX_BALANCE}원을 초과할 수 없습니다: $value" }
    }

    operator fun plus(amount: Long): Balance = Balance(value + amount)
    operator fun minus(amount: Long): Balance = Balance(value - amount)
    operator fun compareTo(other: Balance): Int = value.compareTo(other.value)
    operator fun compareTo(amount: Long): Int = value.compareTo(amount)

    fun canAfford(amount: Long): Boolean = value >= amount
    fun getFormattedBalance(): String = "${String.format("%,d", value)}원"

    companion object {
        private const val MAX_BALANCE = 10_000_000L // 1천만원

        fun of(value: Long): Balance = Balance(value)
        fun zero(): Balance = Balance(0)
    }
}