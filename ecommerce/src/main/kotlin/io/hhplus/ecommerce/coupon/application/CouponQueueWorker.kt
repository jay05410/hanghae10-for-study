package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.util.QueueUtil
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 Queue Worker
 *
 * 역할:
 * - 활성 쿠폰들의 Queue를 주기적으로 처리
 * - Queue에서 요청을 꺼내어 실제 쿠폰 발급 수행
 * - 성공/실패에 따른 Queue 상태 업데이트
 */
@Component
@ConditionalOnProperty(
    prefix = "coupon.queue.worker",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class CouponQueueWorker(
    private val couponQueueService: CouponQueueService,
    private val couponService: CouponService,
    private val couponIssueHistoryService: CouponIssueHistoryService
) {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val PROCESS_INTERVAL_MS = 100L
        private const val MAX_BATCH_SIZE = 50
    }

    /**
     * Queue 처리 메인 루프 (100ms 주기)
     */
    @Scheduled(fixedDelay = PROCESS_INTERVAL_MS)
    fun processQueues() {
        try {
            val activeCoupons = couponService.getAvailableCoupons()

            activeCoupons.forEach { coupon ->
                processQueue(coupon.id)
            }
        } catch (e: Exception) {
            logger.error("Queue 처리 중 예상치 못한 오류", e)
        }
    }

    /**
     * 특정 쿠폰의 Queue 처리
     */
    private fun processQueue(couponId: Long) {
        QueueUtil.processQueueBatch(
            batchSize = MAX_BATCH_SIZE,
            dequeue = { couponQueueService.dequeue(couponId) },
            process = { queueRequest ->
                val coupon = couponService.getCoupon(queueRequest.couponId)!!
                couponService.issueCoupon(coupon, queueRequest.userId)
            },
            onSuccess = { queueRequest, userCoupon ->
                handleSuccess(queueRequest, userCoupon)
            },
            onFailure = { queueRequest, error ->
                val failureReason = error.message ?: "알 수 없는 오류"
                couponQueueService.updateQueueStatus(queueRequest.queueId, QueueStatus.FAILED, failureReason)
                logger.warn(
                    "쿠폰 발급 실패 - queueId: {}, userId: {}, reason: {}",
                    queueRequest.queueId, queueRequest.userId, failureReason, error
                )
            }
        )
    }

    /**
     * 쿠폰 발급 성공 후처리 (분산 트랜잭션)
     *
     * Redis 큐 상태 업데이트와 MySQL 이력 저장을 하나의 트랜잭션으로 처리
     */
    @DistributedTransaction
    fun handleSuccess(queueRequest: CouponQueueRequest, userCoupon: UserCoupon) {
        // 큐 상태 업데이트 (Redis)
        couponQueueService.updateQueueStatus(queueRequest.queueId, QueueStatus.COMPLETED, userCoupon.id)

        // 쿠폰 발급 이력 저장 (MySQL)
        couponIssueHistoryService.recordIssue(
            couponId = queueRequest.couponId,
            userId = queueRequest.userId,
            couponName = queueRequest.couponName
        )

        logger.info(
            "쿠폰 발급 성공 - queueId: {}, userId: {}, userCouponId: {}",
            queueRequest.queueId, queueRequest.userId, userCoupon.id
        )
    }
}