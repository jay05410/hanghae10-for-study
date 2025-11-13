package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import org.springframework.stereotype.Component

/**
 * 재고 예약 UseCase
 *
 * 역할:
 * - 모든 재고 예약 관련 작업을 통합 관리
 * - 재고 예약, 확정, 취소 기능 제공
 *
 * 책임:
 * - 재고 예약 변경 요청 검증 및 실행
 * - 재고 예약 데이터 무결성 보장
 */
@Component
class InventoryReservationUseCase(
    private val stockReservationService: StockReservationService
) {

    /**
     * 지정된 상품의 재고를 사용자에게 예약한다
     *
     * @param productId 예약할 상품 ID
     * @param userId 인증된 사용자 ID
     * @param quantity 예약할 수량 (양수)
     * @param reservationMinutes 예약 유지 시간 (분 단위, 기본 20분)
     * @return 생성된 재고 예약 정보
     * @throws IllegalArgumentException 상품 정보가 유효하지 않거나 재고가 부족한 경우
     * @throws RuntimeException 재고 예약 처리에 실패한 경우
     */
    fun reserveStock(productId: Long, userId: Long, quantity: Int, reservationMinutes: Int = 20): StockReservation {
        return stockReservationService.reserveStock(productId, userId, quantity, reservationMinutes)
    }

    /**
     * 사용자의 재고 예약을 확정하고 실제 재고 사용으로 전환한다
     *
     * @param reservationId 확정할 재고 예약 ID
     * @param userId 인증된 사용자 ID
     * @return 확정 처리된 재고 예약 정보
     * @throws IllegalArgumentException 예약을 찾을 수 없거나 확정 권한이 없는 경우
     * @throws RuntimeException 예약 확정 처리에 실패한 경우
     */
    fun confirmReservation(reservationId: Long, userId: Long): StockReservation {
        return stockReservationService.confirmReservation(reservationId, userId)
    }

    /**
     * 사용자의 재고 예약을 취소하고 예약된 재고를 복원한다
     *
     * @param reservationId 취소할 재고 예약 ID
     * @param userId 인증된 사용자 ID
     * @return 취소 처리된 재고 예약 정보
     * @throws IllegalArgumentException 예약을 찾을 수 없거나 취소 권한이 없는 경우
     * @throws RuntimeException 예약 취소 처리에 실패한 경우
     */
    fun cancelReservation(reservationId: Long, userId: Long): StockReservation {
        return stockReservationService.cancelReservation(reservationId, userId)
    }
}