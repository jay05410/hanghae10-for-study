package io.hhplus.ecommerce.inventory.infra.persistence.repository

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface StockReservationJpaRepository : JpaRepository<StockReservation, Long> {

    fun findByUserIdAndProductIdAndStatus(userId: Long, productId: Long, status: ReservationStatus): StockReservation?

    @Query("SELECT s FROM StockReservation s WHERE s.expiresAt < :expiredBefore AND s.status = 'RESERVED'")
    fun findExpiredReservations(expiredBefore: LocalDateTime): List<StockReservation>

    fun findByUserIdAndStatus(userId: Long, status: ReservationStatus): List<StockReservation>
}