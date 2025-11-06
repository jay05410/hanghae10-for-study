package io.hhplus.ecommerce.domain.order.vo

data class OrderAmount(
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long
) {
    init {
        require(totalAmount >= 0) { "총 금액은 0 이상이어야 합니다" }
        require(discountAmount >= 0) { "할인 금액은 0 이상이어야 합니다" }
        require(finalAmount >= 0) { "최종 금액은 0 이상이어야 합니다" }
        require(finalAmount == totalAmount - discountAmount) { "최종 금액이 올바르지 않습니다" }
    }

    fun hasDiscount(): Boolean = discountAmount > 0

    fun getDiscountRate(): Double = if (totalAmount > 0) {
        (discountAmount.toDouble() / totalAmount.toDouble()) * 100
    } else {
        0.0
    }
}