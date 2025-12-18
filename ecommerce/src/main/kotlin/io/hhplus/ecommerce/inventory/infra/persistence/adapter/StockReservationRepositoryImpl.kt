package io.hhplus.ecommerce.inventory.infra.persistence.adapter

import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.infra.persistence.repository.StockReservationJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * StockReservation Repository JPA 구현체 - 인프라 계층
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository를 사용하여 영속성 처리
 *
 * 특징:
 * - StockReservation은 별도 JPA 엔티티 없이 도메인 엔티티를 직접 사용
 */
@Repository
class StockReservationRepositoryImpl(
    private val jpaRepository: StockReservationJpaRepository
) : StockReservationRepository {

    override fun save(stockReservation: StockReservation): StockReservation =
        jpaRepository.save(stockReservation)

    override fun findById(id: Long): StockReservation? =
        jpaRepository.findById(id).orElse(null)

    override fun findByOrderId(orderId: Long): List<StockReservation> =
        jpaRepository.findByOrderId(orderId)

    override fun findByUserIdAndProductIdAndStatus(
        userId: Long,
        productId: Long,
        status: ReservationStatus
    ): StockReservation? =
        jpaRepository.findByUserIdAndProductIdAndStatus(userId, productId, status)

    override fun findExpiredReservations(expiredBefore: LocalDateTime): List<StockReservation> =
        jpaRepository.findExpiredReservations(expiredBefore)

    override fun findByUserIdAndStatus(userId: Long, status: ReservationStatus): List<StockReservation> =
        jpaRepository.findByUserIdAndStatus(userId, status)

    override fun delete(stockReservation: StockReservation) {
        jpaRepository.delete(stockReservation)
    }

    override fun deleteExpiredReservations(expiredBefore: LocalDateTime): Int {
        val completedReservations = jpaRepository.findOldCompletedReservations(expiredBefore)
        jpaRepository.deleteAll(completedReservations)
        return completedReservations.size
    }
}
