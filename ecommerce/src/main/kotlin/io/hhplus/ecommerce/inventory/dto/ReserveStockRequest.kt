package io.hhplus.ecommerce.inventory.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "재고 예약 요청")
data class ReserveStockRequest(
    @Schema(description = "예약할 수량", example = "5", required = true)
    val quantity: Int,

    @Schema(description = "예약 유지 시간 (분)", example = "20", defaultValue = "20")
    val reservationMinutes: Int? = 20
)