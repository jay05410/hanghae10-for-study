package io.hhplus.ecommerce.inventory.domain.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "stock_reservations")
class StockReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: ReservationStatus = ReservationStatus.RESERVED,

    @Column(nullable = false)
    val reservedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(20),

    @Version
    var version: Int = 0
) : BaseJpaEntity() {

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isReservationActive(): Boolean = status == ReservationStatus.RESERVED && !isExpired()

    fun confirm() {
        require(isReservationActive()) { "예약이 활성 상태가 아닙니다" }
        this.status = ReservationStatus.CONFIRMED
    }

    fun cancel() {
        require(status == ReservationStatus.RESERVED) { "예약 상태에서만 취소할 수 있습니다" }
        this.status = ReservationStatus.CANCELLED
    }

    fun expire() {
        require(status == ReservationStatus.RESERVED) { "예약 상태에서만 만료할 수 있습니다" }
        this.status = ReservationStatus.EXPIRED
    }

    companion object {
        fun create(
            productId: Long,
            userId: Long,
            quantity: Int,
            reservationMinutes: Int = 20
        ): StockReservation {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(quantity > 0) { "예약 수량은 0보다 커야 합니다" }
            require(reservationMinutes > 0) { "예약 시간은 0보다 커야 합니다" }

            return StockReservation(
                productId = productId,
                userId = userId,
                quantity = quantity,
                expiresAt = LocalDateTime.now().plusMinutes(reservationMinutes.toLong())
            )
        }
    }
}