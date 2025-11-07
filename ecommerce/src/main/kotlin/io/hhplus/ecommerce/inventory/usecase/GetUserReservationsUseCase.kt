package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import org.springframework.stereotype.Component

/**
 * 사용자 재고 예약 조회 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자의 재고 예약 내역 조회
 * - 예약 상태별 필터링 및 데이터 제공
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 사용자 권한 기반 예약 내역 조회
 * - 예약 데이터 변환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetUserReservationsUseCase(
    private val stockReservationService: StockReservationService
) {

    /**
     * 사용자의 모든 재고 예약 내역을 조회한다
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 재고 예약 목록 (모든 상태 포함)
     */
    fun execute(userId: Long): List<StockReservation> {
        return stockReservationService.getUserReservations(userId)
    }
}