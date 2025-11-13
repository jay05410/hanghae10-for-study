package io.hhplus.ecommerce.inventory.dto

import io.swagger.v3.oas.annotations.media.Schema

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import java.time.LocalDateTime

@Schema(description = "재고 예약 정보")
data class StockReservationResponse(
    @Schema(description = "예약 ID", example = "1")
    val id: Long,

    @Schema(description = "상품 ID", example = "10")
    val productId: Long,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "예약 수량", example = "5")
    val quantity: Int,

    @Schema(description = "예약 상태", example = "RESERVED", allowableValues = ["RESERVED", "CONFIRMED", "CANCELLED", "EXPIRED"])
    val status: ReservationStatus,

    @Schema(description = "예약 일시", example = "2025-01-13T10:00:00")
    val reservedAt: LocalDateTime,

    @Schema(description = "만료 일시", example = "2025-01-13T10:05:00")
    val expiresAt: LocalDateTime,

    @Schema(description = "예약 활성화 상태", example = "true")
    val isActive: Boolean,

    @Schema(description = "생성 일시", example = "2025-01-13T10:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정 일시", example = "2025-01-13T10:02:00")
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