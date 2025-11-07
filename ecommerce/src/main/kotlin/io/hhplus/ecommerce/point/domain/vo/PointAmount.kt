package io.hhplus.ecommerce.point.domain.vo

data class PointAmount(
    val value: Long
) {
    init {
        require(value >= 0) { "포인트 금액은 0 이상이어야 합니다: $value" }
    }

    companion object {
        fun of(amount: Long): PointAmount {
            return PointAmount(amount)
        }
    }
}