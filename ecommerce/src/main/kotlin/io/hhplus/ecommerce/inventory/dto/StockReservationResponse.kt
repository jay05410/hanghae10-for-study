package io.hhplus.ecommerce.inventory.dto

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import java.time.LocalDateTime

data class StockReservationResponse(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val quantity: Int,
    val status: ReservationStatus,
    val reservedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

fun StockReservation.toResponse(): StockReservationResponse = StockReservationResponse(
    id = this.id,
    productId = this.productId,
    userId = this.userId,
    quantity = this.quantity,
    status = this.status,
    reservedAt = this.reservedAt,
    expiresAt = this.expiresAt,
    isActive = this.isReservationActive(),
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)