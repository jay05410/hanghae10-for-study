package io.hhplus.ecommerce.point.dto

data class DeductPointRequest(
    val amount: Long,
    val description: String? = null
)