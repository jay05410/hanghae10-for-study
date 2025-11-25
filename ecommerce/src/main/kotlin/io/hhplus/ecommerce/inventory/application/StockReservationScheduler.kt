package io.hhplus.ecommerce.inventory.application

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StockReservationScheduler(
    private val stockReservationService: StockReservationService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 매분마다 만료된 예약을 확인하고 해제
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    fun expireReservations() {
        try {
            val expiredCount = stockReservationService.expireReservations()
            if (expiredCount > 0) {
                logger.info("Expired {} stock reservations", expiredCount)
            }
        } catch (e: Exception) {
            logger.error("Failed to expire stock reservations", e)
        }
    }
}