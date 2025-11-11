package io.hhplus.ecommerce.inventory.domain.repository

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import java.time.LocalDateTime

interface StockReservationRepository {
    fun save(stockReservation: StockReservation): StockReservation
    fun findById(id: Long): StockReservation?
    fun findByUserIdAndProductIdAndStatus(userId: Long, productId: Long, status: ReservationStatus): StockReservation?
    fun findExpiredReservations(expiredBefore: LocalDateTime): List<StockReservation>
    fun findByUserIdAndStatus(userId: Long, status: ReservationStatus): List<StockReservation>
    fun delete(stockReservation: StockReservation)
}