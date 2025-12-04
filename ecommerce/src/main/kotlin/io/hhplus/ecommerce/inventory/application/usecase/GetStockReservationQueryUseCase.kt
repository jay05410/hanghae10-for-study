package io.hhplus.ecommerce.inventory.application.usecase

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.service.StockReservationDomainService
import org.springframework.stereotype.Component

/**
 * 재고 예약 조회 UseCase - 애플리케이션 계층 (Query)
 *
 * 역할:
 * - 재고 예약 관련 조회 작업 처리
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 사용자별 예약 현황 조회
 * - 개별 예약 정보 조회
 */
@Component
class GetStockReservationQueryUseCase(
    private val stockReservationDomainService: StockReservationDomainService
) {

    /**
     * 사용자의 활성 예약 목록 조회
     */
    fun getUserActiveReservations(userId: Long): List<StockReservation> {
        return stockReservationDomainService.getUserActiveReservations(userId)
    }

    /**
     * 예약 ID로 예약 조회
     */
    fun getReservation(reservationId: Long): StockReservation? {
        return stockReservationDomainService.getReservation(reservationId)
    }
}
