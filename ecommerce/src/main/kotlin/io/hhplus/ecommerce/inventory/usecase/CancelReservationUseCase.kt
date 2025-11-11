package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import org.springframework.stereotype.Component

/**
 * 재고 예약 취소 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자의 재고 예약을 취소하는 비즈니스 플로우 수행
 * - 예약 취소 권한 검증 및 재고 복원 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 예약 소유권 및 취소 가능 상태 검증
 * - 예약 취소 트랜잭션 관리
 * - 취소된 재고량 시스템 복원
 */
@Component
class CancelReservationUseCase(
    private val stockReservationService: StockReservationService
) {

    /**
     * 사용자의 재고 예약을 취소하고 예약된 재고를 복원한다
     *
     * @param reservationId 취소할 재고 예약 ID
     * @param userId 인증된 사용자 ID
     * @return 취소 처리된 재고 예약 정보
     * @throws IllegalArgumentException 예약을 찾을 수 없거나 취소 권한이 없는 경우
     * @throws RuntimeException 예약 취소 처리에 실패한 경우
     */
    fun execute(reservationId: Long, userId: Long): StockReservation {
        return stockReservationService.cancelReservation(reservationId, userId)
    }
}