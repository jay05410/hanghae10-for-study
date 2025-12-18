package io.hhplus.ecommerce.inventory.domain.service

import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.exception.InventoryException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 재고 예약 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 재고 예약 엔티티 생성 및 상태 관리
 * - 예약 확정, 취소, 만료 처리
 * - 사용자별 예약 조회
 *
 * 책임:
 * - 재고 예약 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - @Transactional 사용 금지 (UseCase에서 관리)
 * - 재고 수량 변경은 InventoryDomainService에서 담당
 */
@Component
class StockReservationDomainService(
    private val stockReservationRepository: StockReservationRepository
) {

    /**
     * 예약 ID로 예약 조회
     */
    fun getReservation(reservationId: Long): StockReservation? {
        return stockReservationRepository.findById(reservationId)
    }

    /**
     * 예약 ID로 예약 조회 (없으면 예외)
     */
    fun getReservationOrThrow(reservationId: Long): StockReservation {
        return stockReservationRepository.findById(reservationId)
            ?: throw InventoryException.ReservationNotFound(reservationId)
    }

    /**
     * 사용자의 특정 상품에 대한 활성 예약 조회
     */
    fun findActiveReservation(userId: Long, productId: Long): StockReservation? {
        return stockReservationRepository.findByUserIdAndProductIdAndStatus(
            userId, productId, ReservationStatus.RESERVED
        )
    }

    /**
     * 주문 ID로 예약 목록 조회
     */
    fun findByOrderId(orderId: Long): List<StockReservation> {
        return stockReservationRepository.findByOrderId(orderId)
    }

    /**
     * 예약 저장
     */
    fun save(reservation: StockReservation): StockReservation {
        return stockReservationRepository.save(reservation)
    }

    /**
     * 새 예약 생성
     */
    fun createReservation(
        productId: Long,
        userId: Long,
        quantity: Int,
        reservationMinutes: Int
    ): StockReservation {
        val requiresReservation = reservationMinutes > StockReservation.DEFAULT_RESERVATION_MINUTES
        val reservation = StockReservation.create(
            productId = productId,
            userId = userId,
            quantity = quantity,
            requiresReservation = requiresReservation
        )
        return stockReservationRepository.save(reservation)
    }

    /**
     * 예약 확정
     */
    fun confirmReservation(reservation: StockReservation): StockReservation {
        reservation.confirm()
        return stockReservationRepository.save(reservation)
    }

    /**
     * 예약 취소
     */
    fun cancelReservation(reservation: StockReservation): StockReservation {
        reservation.cancel()
        return stockReservationRepository.save(reservation)
    }

    /**
     * 예약 만료
     */
    fun expireReservation(reservation: StockReservation): StockReservation {
        reservation.expire()
        return stockReservationRepository.save(reservation)
    }

    /**
     * 예약 접근 권한 검증
     */
    fun validateReservationAccess(reservation: StockReservation, userId: Long) {
        if (reservation.userId != userId) {
            throw InventoryException.ReservationAccessDenied(reservation.id, userId)
        }
    }

    /**
     * 예약 활성 상태 검증
     */
    fun validateReservationActive(reservation: StockReservation) {
        if (!reservation.isReservationActive()) {
            throw InventoryException.ReservationExpired(reservation.id)
        }
    }

    /**
     * 예약 취소 가능 상태 검증
     */
    fun validateReservationCancellable(reservation: StockReservation) {
        if (reservation.status != ReservationStatus.RESERVED) {
            throw InventoryException.ReservationCannotBeCancelled(reservation.id, reservation.status)
        }
    }

    /**
     * 사용자의 활성 예약 목록 조회
     */
    fun getUserActiveReservations(userId: Long): List<StockReservation> {
        return stockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED)
            .filter { it.isReservationActive() }
    }

    /**
     * 만료된 예약 목록 조회
     */
    fun findExpiredReservations(): List<StockReservation> {
        return stockReservationRepository.findExpiredReservations(LocalDateTime.now())
    }

    /**
     * 오래된 예약 삭제
     */
    fun cleanupOldReservations(daysOld: Long): Int {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld)
        return stockReservationRepository.deleteExpiredReservations(cutoffDate)
    }
}
