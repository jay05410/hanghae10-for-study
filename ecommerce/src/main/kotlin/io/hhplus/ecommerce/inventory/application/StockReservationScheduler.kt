package io.hhplus.ecommerce.inventory.application

import io.hhplus.ecommerce.inventory.application.usecase.StockReservationCommandUseCase
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 재고 예약 스케줄러 - 애플리케이션 계층
 *
 * 역할:
 * - 만료된 재고 예약의 자동 해제
 * - 주기적인 배치 작업 수행
 *
 * 책임:
 * - UseCase를 통해 만료 예약 처리 위임
 */
@Component
class StockReservationScheduler(
    private val stockReservationCommandUseCase: StockReservationCommandUseCase
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 매분마다 만료된 예약을 확인하고 해제
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    fun expireReservations() {
        try {
            val expiredCount = stockReservationCommandUseCase.expireReservations()
            if (expiredCount > 0) {
                logger.info("Expired {} stock reservations", expiredCount)
            }
        } catch (e: Exception) {
            logger.error("Failed to expire stock reservations", e)
        }
    }
}