package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.common.queue.QueueProcessor
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 Queue Processor
 */
@Component
class CouponQueueProcessor(
    private val couponQueueService: CouponQueueService,
    private val couponService: CouponService
) : QueueProcessor<CouponQueueRequest, UserCoupon> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var currentCouponId: Long? = null

    fun setCouponId(couponId: Long) {
        this.currentCouponId = couponId
    }

    override fun dequeue(): CouponQueueRequest? {
        val couponId = currentCouponId ?: return null
        return couponQueueService.dequeue(couponId)
    }

    override fun process(item: CouponQueueRequest): UserCoupon {
        logger.info(
            "쿠폰 발급 Queue 처리 시작 - queueId: {}, userId: {}, couponId: {}, position: {}",
            item.queueId, item.userId, item.couponId, item.queuePosition
        )

        return couponService.issueCoupon(
            userId = item.userId,
            couponId = item.couponId
        )
    }

    override fun onSuccess(item: CouponQueueRequest, result: UserCoupon) {
        couponQueueService.completeQueue(
            queueId = item.queueId,
            userCouponId = result.id
        )

        logger.info(
            "쿠폰 발급 성공 - queueId: {}, userId: {}, userCouponId: {}",
            item.queueId, item.userId, result.id
        )
    }

    override fun onFailure(item: CouponQueueRequest, error: Exception) {
        val failureReason = error.message ?: "알 수 없는 오류"

        couponQueueService.failQueue(
            queueId = item.queueId,
            reason = failureReason
        )

        logger.warn(
            "쿠폰 발급 실패 - queueId: {}, userId: {}, reason: {}",
            item.queueId, item.userId, failureReason, error
        )
    }
}
