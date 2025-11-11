package io.hhplus.ecommerce.point.dto

data class ChargePointRequest(
    val amount: Long,
    val description: String? = null
)