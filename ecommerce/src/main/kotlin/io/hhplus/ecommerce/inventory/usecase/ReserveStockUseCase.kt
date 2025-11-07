package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import org.springframework.stereotype.Component

/**
 * 재고 예약 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 재고 예약 비즈니스 플로우 수행
 * - 재고 가용성 검증 및 예약 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 상품 재고 가용성 및 수량 유효성 검증
 * - 재고 예약 생성 트랜잭션 관리
 * - 예약 만료 시간 설정 및 시간 제한 관리
 */
@Component
class ReserveStockUseCase(
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
    fun execute(productId: Long, userId: Long, quantity: Int, reservationMinutes: Int = 20): StockReservation {
        return stockReservationService.reserveStock(productId, userId, quantity, reservationMinutes)
    }
}