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
) {
    companion object {
        fun from(reservation: StockReservation): StockReservationResponse {
            return StockReservationResponse(
                id = reservation.id,
                productId = reservation.productId,
                userId = reservation.userId,
                quantity = reservation.quantity,
                status = reservation.status,
                reservedAt = reservation.reservedAt,
                expiresAt = reservation.expiresAt,
                isActive = reservation.isReservationActive(),
                createdAt = reservation.createdAt,
                updatedAt = reservation.updatedAt
            )
        }
    }
}