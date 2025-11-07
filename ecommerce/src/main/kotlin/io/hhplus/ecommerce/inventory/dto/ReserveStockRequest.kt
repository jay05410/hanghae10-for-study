package io.hhplus.ecommerce.inventory.dto

data class ReserveStockRequest(
    val quantity: Int,
    val reservationMinutes: Int? = 20
)