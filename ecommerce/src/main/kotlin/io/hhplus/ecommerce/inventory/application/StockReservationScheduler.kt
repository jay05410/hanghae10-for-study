package io.hhplus.ecommerce.inventory.application

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.inventory.application.usecase.StockReservationCommandUseCase
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 재고 예약 스케줄러 - 애플리케이션 계층
 *
 * 역할:
 * - 만료된 재고 예약의 자동 해제
 * - 연결된 주문의 만료 처리 (PENDING_PAYMENT → EXPIRED)
 * - 주기적인 배치 작업 수행
 *
 * 책임:
 * - UseCase를 통해 만료 예약 처리 위임
 * - 분산 락을 통해 멀티 인스턴스 환경에서 중복 실행 방지
 */
@Component
class StockReservationScheduler(
    private val stockReservationCommandUseCase: StockReservationCommandUseCase
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 매분마다 만료된 예약을 확인하고 해제
     *
     * 처리 내용:
     * 1. 만료된 재고 예약 조회
     * 2. 재고 예약 해제
     * 3. 예약 상태 만료 처리
     * 4. 연결된 주문 만료 처리 (PENDING_PAYMENT → EXPIRED)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @DistributedLock(key = "'scheduler:reservation-expiry'", leaseTime = 55L)
    fun expireReservations() {
        try {
            val expiredCount = stockReservationCommandUseCase.expireReservations()
            if (expiredCount > 0) {
                logger.info("[ReservationScheduler] 만료 예약 처리 완료: count=$expiredCount")
            }
        } catch (e: Exception) {
            logger.error("[ReservationScheduler] 만료 예약 처리 실패", e)
        }
    }
}