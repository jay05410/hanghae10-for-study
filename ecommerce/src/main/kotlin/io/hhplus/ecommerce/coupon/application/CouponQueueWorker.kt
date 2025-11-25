package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.common.queue.BaseQueueWorker
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 Queue Worker
 */
@Component
@ConditionalOnProperty(
    prefix = "coupon.queue.worker",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class CouponQueueWorker(
    private val couponQueueProcessor: CouponQueueProcessor,
    private val couponRepository: CouponRepository
) : BaseQueueWorker<CouponQueueRequest, UserCoupon>(couponQueueProcessor) {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val PROCESS_INTERVAL_MS = 100L
    }

    /**
     * Queue 처리 메인 루프 (100ms 주기)
     */
    @Scheduled(fixedDelay = PROCESS_INTERVAL_MS)
    fun process() {
        try {
            val coupons = couponRepository.findByIsActiveTrue()

            for (coupon in coupons) {
                couponQueueProcessor.setCouponId(coupon.id)
                processQueue()
            }
        } catch (e: Exception) {
            logger.error("Queue 처리 중 예상치 못한 오류", e)
        }
    }
}
