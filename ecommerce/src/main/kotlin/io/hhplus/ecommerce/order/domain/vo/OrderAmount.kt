package io.hhplus.ecommerce.order.domain.vo

@JvmInline
value class TotalAmount private constructor(val value: Long) {
    init {
        require(value >= 0) { "총 금액은 0 이상이어야 합니다: $value" }
    }

    companion object {
        fun of(value: Long): TotalAmount = TotalAmount(value)
    }
}

@JvmInline
value class DiscountAmount private constructor(val value: Long) {
    init {
        require(value >= 0) { "할인 금액은 0 이상이어야 합니다: $value" }
    }

    companion object {
        fun of(value: Long): DiscountAmount = DiscountAmount(value)
        fun zero(): DiscountAmount = DiscountAmount(0)
    }
}

@JvmInline
value class FinalAmount private constructor(val value: Long) {
    init {
        require(value >= 0) { "최종 금액은 0 이상이어야 합니다: $value" }
    }

    companion object {
        fun of(value: Long): FinalAmount = FinalAmount(value)
        fun calculate(totalAmount: TotalAmount, discountAmount: DiscountAmount): FinalAmount {
            val finalValue = totalAmount.value - discountAmount.value
            require(finalValue >= 0) { "최종 금액이 음수가 될 수 없습니다" }
            return FinalAmount(finalValue)
        }
    }
}

data class OrderAmount(
    val totalAmount: TotalAmount,
    val discountAmount: DiscountAmount,
    val finalAmount: FinalAmount
) {
    init {
        require(finalAmount.value == totalAmount.value - discountAmount.value) {
            "최종 금액이 올바르지 않습니다"
        }
    }

    fun hasDiscount(): Boolean = discountAmount.value > 0

    fun getDiscountRate(): Double = if (totalAmount.value > 0) {
        (discountAmount.value.toDouble() / totalAmount.value.toDouble()) * 100
    } else {
        0.0
    }

    companion object {
        fun of(totalAmount: Long, discountAmount: Long = 0): OrderAmount {
            val total = TotalAmount.of(totalAmount)
            val discount = DiscountAmount.of(discountAmount)
            val final = FinalAmount.calculate(total, discount)
            return OrderAmount(total, discount, final)
        }
    }
}