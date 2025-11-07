package io.hhplus.ecommerce.payment.domain.constant

enum class TransactionType {
    CHARGE, USE;

    fun isChargeType(): Boolean = this == CHARGE
    fun isUseType(): Boolean = this == USE
}