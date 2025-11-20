package io.hhplus.ecommerce.inventory.application

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.inventory.exception.InventoryException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 재고 예약 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 재고 예약 시스템 관리
 * - 임시 재고 예약 및 확정/취소 프로세스
 * - 예약 만료 처리 및 자동 해제
 *
 * 책임:
 * - 재고 예약 생성, 확정, 취소 관리
 * - 예약 만료 처리 및 배치 작업
 * - 사용자별 예약 현황 조회
 */
@Service
class StockReservationService(
    private val stockReservationRepository: StockReservationRepository,
    private val inventoryRepository: InventoryRepository
) {

    @Transactional
    fun reserveStock(productId: Long, userId: Long, quantity: Int, reservationMinutes: Int = 20): StockReservation {
        // 이미 해당 상품에 예약이 있는지 확인
        val existingReservation = stockReservationRepository.findByUserIdAndProductIdAndStatus(
            userId, productId, ReservationStatus.RESERVED
        )

        if (existingReservation != null && existingReservation.isReservationActive()) {
            throw InventoryException.StockAlreadyReserved(productId, userId)
        }

        // 재고 확인 및 예약
        val inventory = inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)

        inventory.reserve(quantity)
        inventoryRepository.save(inventory)

        // 예약 기록 생성
        val reservation = StockReservation.create(
            productId = productId,
            userId = userId,
            quantity = quantity,
            reservationMinutes = reservationMinutes
        )

        return stockReservationRepository.save(reservation)
    }

    @Transactional
    fun confirmReservation(reservationId: Long, userId: Long): StockReservation {
        val reservation = stockReservationRepository.findById(reservationId)
            ?: throw InventoryException.ReservationNotFound(reservationId)

        if (reservation.userId != userId) {
            throw InventoryException.ReservationAccessDenied(reservationId, userId)
        }

        if (!reservation.isReservationActive()) {
            throw InventoryException.ReservationExpired(reservationId)
        }

        // 재고에서 예약 확정 (실제 차감)
        val inventory = inventoryRepository.findByProductIdWithLock(reservation.productId)
            ?: throw InventoryException.InventoryNotFound(reservation.productId)

        inventory.confirmReservation(reservation.quantity)
        inventoryRepository.save(inventory)

        // 예약 상태 변경
        reservation.confirm()
        return stockReservationRepository.save(reservation)
    }

    @Transactional
    fun cancelReservation(reservationId: Long, userId: Long): StockReservation {
        val reservation = stockReservationRepository.findById(reservationId)
            ?: throw InventoryException.ReservationNotFound(reservationId)

        if (reservation.userId != userId) {
            throw InventoryException.ReservationAccessDenied(reservationId, userId)
        }

        if (reservation.status != ReservationStatus.RESERVED) {
            throw InventoryException.ReservationCannotBeCancelled(reservationId, reservation.status)
        }

        // 재고 예약 해제
        val inventory = inventoryRepository.findByProductIdWithLock(reservation.productId)
            ?: throw InventoryException.InventoryNotFound(reservation.productId)

        inventory.releaseReservation(reservation.quantity)
        inventoryRepository.save(inventory)

        // 예약 취소
        reservation.cancel()
        return stockReservationRepository.save(reservation)
    }

    @Transactional
    fun expireReservations(): Int {
        val expiredReservations = stockReservationRepository.findExpiredReservations(LocalDateTime.now())
        var expiredCount = 0

        expiredReservations.forEach { reservation ->
            try {
                // 재고 예약 해제
                val inventory = inventoryRepository.findByProductIdWithLock(reservation.productId)
                if (inventory != null) {
                    inventory.releaseReservation(reservation.quantity) // 시스템에 의한 해제
                    inventoryRepository.save(inventory)
                }

                // 예약 만료 처리
                reservation.expire()
                stockReservationRepository.save(reservation)
                expiredCount++
            } catch (e: Exception) {
                // 로깅만 하고 다음 예약 처리 계속
                println("Failed to expire reservation ${reservation.id}: ${e.message}")
            }
        }

        return expiredCount
    }

    fun getUserReservations(userId: Long): List<StockReservation> {
        return stockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED)
            .filter { it.isReservationActive() }
    }

    /**
     * 완전히 처리된 예약 데이터를 물리 삭제
     * 배치 작업에서 호출하여 오래된 예약 데이터를 정리
     *
     * @param daysOld 삭제할 예약 데이터의 최소 경과 일수 (기본 30일)
     * @return 삭제된 예약 개수
     */
    @Transactional
    fun cleanupOldReservations(daysOld: Long = 30): Int {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld)
        return stockReservationRepository.deleteExpiredReservations(cutoffDate)
    }
}