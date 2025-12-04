package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 쿠폰 발급 Worker
 *
 * 역할:
 * - 활성 쿠폰들의 발급 대기열을 주기적으로 처리
 * - ZPOPMIN으로 선착순 순서대로 유저를 꺼내어 쿠폰 발급
 *
 * Redis 자료구조:
 * - ZSET: 발급 대기열에서 ZPOPMIN으로 배치 처리
 *
 */
@Component
@ConditionalOnProperty(
    prefix = "coupon.issue.worker",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class CouponIssueWorker(
    private val couponIssueService: CouponIssueService,
    private val couponDomainService: CouponDomainService,
    private val couponIssueHistoryService: CouponIssueHistoryService
) {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val PROCESS_INTERVAL_MS = 100L
        private const val MAX_BATCH_SIZE = 50
    }

    /**
     * 발급 대기열 처리 메인 루프 (100ms 주기)
     *
     * 활성 쿠폰들의 대기열을 순회하며 배치 처리.
     */
    @Scheduled(fixedDelay = PROCESS_INTERVAL_MS)
    fun processPendingIssues() {
        try {
            val activeCoupons = couponDomainService.getAvailableCoupons()

            activeCoupons.forEach { coupon ->
                processCouponIssues(coupon.id, coupon.name)
            }
        } catch (e: Exception) {
            logger.error("발급 대기열 처리 중 예상치 못한 오류", e)
        }
    }

    /**
     * 특정 쿠폰의 발급 대기열 처리
     *
     * ZPOPMIN으로 배치 크기만큼 유저를 꺼내어 발급 처리.
     */
    private fun processCouponIssues(couponId: Long, couponName: String) {
        // ZPOPMIN으로 대기열에서 유저들을 꺼냄
        val userIds = couponIssueService.popPendingUsers(couponId, MAX_BATCH_SIZE)

        if (userIds.isEmpty()) {
            return
        }

        userIds.forEach { userId ->
            try {
                val coupon = couponDomainService.getCouponOrThrow(couponId)
                val userCoupon = couponDomainService.issueCoupon(coupon, userId)
                handleSuccess(couponId, userId, couponName, userCoupon)
            } catch (e: Exception) {
                handleFailure(couponId, userId, e)
            }
        }
    }

    /**
     * 쿠폰 발급 성공 후처리
     */
    @DistributedTransaction
    fun handleSuccess(couponId: Long, userId: Long, couponName: String, userCoupon: UserCoupon) {
        // 쿠폰 발급 이력 저장 (MySQL)
        couponIssueHistoryService.recordIssue(
            couponId = couponId,
            userId = userId,
            couponName = couponName
        )

        logger.info(
            "쿠폰 발급 성공 - couponId: {}, userId: {}, userCouponId: {}",
            couponId, userId, userCoupon.id
        )
    }

    /**
     * 쿠폰 발급 실패 처리
     */
    private fun handleFailure(couponId: Long, userId: Long, error: Exception) {
        val failureReason = error.message ?: "알 수 없는 오류"
        logger.warn(
            "쿠폰 발급 실패 - couponId: {}, userId: {}, reason: {}",
            couponId, userId, failureReason, error
        )
    }
}
