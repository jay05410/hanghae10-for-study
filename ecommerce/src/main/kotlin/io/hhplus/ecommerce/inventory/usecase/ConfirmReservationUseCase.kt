package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import org.springframework.stereotype.Component

/**
 * 재고 예약 확정 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자의 재고 예약을 확정하는 비즈니스 플로우 수행
 * - 예약된 재고를 실제 사용 상태로 전환
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 예약 소유권 및 확정 가능 상태 검증
 * - 예약 확정 트랜잭션 관리
 * - 재고 예약에서 실제 사용으로 상태 변경
 */
@Component
class ConfirmReservationUseCase(
    private val stockReservationService: StockReservationService
) {

    /**
     * 사용자의 재고 예약을 확정하고 실제 재고 사용으로 전환한다
     *
     * @param reservationId 확정할 재고 예약 ID
     * @param userId 인증된 사용자 ID
     * @return 확정 처리된 재고 예약 정보
     * @throws IllegalArgumentException 예약을 찾을 수 없거나 확정 권한이 없는 경우
     * @throws RuntimeException 예약 확정 처리에 실패한 경우
     */
    fun execute(reservationId: Long, userId: Long): StockReservation {
        return stockReservationService.confirmReservation(reservationId, userId)
    }
}