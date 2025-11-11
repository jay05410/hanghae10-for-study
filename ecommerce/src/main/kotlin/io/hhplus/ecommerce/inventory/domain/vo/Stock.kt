package io.hhplus.ecommerce.inventory.domain.vo

@JvmInline
value class Stock private constructor(val value: Int) {
    init {
        require(value >= 0) { "재고는 0 이상이어야 합니다: $value" }
        require(value <= MAX_STOCK) { "재고는 ${MAX_STOCK}개를 초과할 수 없습니다: $value" }
    }

    operator fun plus(quantity: Int): Stock = Stock(value + quantity)
    operator fun minus(quantity: Int): Stock = Stock(value - quantity)
    operator fun compareTo(other: Stock): Int = value.compareTo(other.value)
    operator fun compareTo(quantity: Int): Int = value.compareTo(quantity)

    fun canReserve(quantity: Int): Boolean = value >= quantity
    fun isOutOfStock(): Boolean = value == 0
    fun isLowStock(): Boolean = value <= LOW_STOCK_THRESHOLD

    companion object {
        private const val MAX_STOCK = 99999
        private const val LOW_STOCK_THRESHOLD = 10

        fun of(value: Int): Stock = Stock(value)
        fun zero(): Stock = Stock(0)
    }
}