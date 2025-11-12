package io.hhplus.ecommerce.inventory.infra

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.inventory.infra.persistence.repository.StockReservationJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * StockReservation Repository JPA 구현체
 */
@Repository
class StockReservationRepositoryImpl(
    private val jpaRepository: StockReservationJpaRepository
) : StockReservationRepository {

    override fun save(stockReservation: StockReservation): StockReservation =
        jpaRepository.save(stockReservation)

    override fun findById(id: Long): StockReservation? =
        jpaRepository.findById(id).orElse(null)

    override fun findByUserIdAndProductIdAndStatus(userId: Long, productId: Long, status: ReservationStatus): StockReservation? =
        jpaRepository.findByUserIdAndProductIdAndStatus(userId, productId, status)

    override fun findExpiredReservations(expiredBefore: LocalDateTime): List<StockReservation> =
        jpaRepository.findExpiredReservations(expiredBefore)

    override fun findByUserIdAndStatus(userId: Long, status: ReservationStatus): List<StockReservation> =
        jpaRepository.findByUserIdAndStatus(userId, status)

    override fun delete(stockReservation: StockReservation) {
        jpaRepository.delete(stockReservation)
    }
}