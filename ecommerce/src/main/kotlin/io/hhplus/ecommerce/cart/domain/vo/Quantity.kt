package io.hhplus.ecommerce.cart.domain.vo

@JvmInline
value class Quantity private constructor(val value: Int) {
    init {
        require(value > 0) { "수량은 0보다 커야 합니다: $value" }
        require(value <= MAX_QUANTITY) { "수량은 ${MAX_QUANTITY}개를 초과할 수 없습니다: $value" }
    }

    operator fun plus(other: Quantity): Quantity = Quantity(value + other.value)
    operator fun minus(other: Quantity): Quantity = Quantity(value - other.value)
    operator fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    companion object {
        private const val MAX_QUANTITY = 999

        fun of(value: Int): Quantity = Quantity(value)
        fun one(): Quantity = Quantity(1)
    }
}