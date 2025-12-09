package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
 * 개선 사항 (피드백 반영):
 * - 100ms → 500ms 주기로 변경 (DB 부하 감소)
 * - 단건 처리 → Bulk 처리 (saveAll 활용)
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
        // 100ms → 500ms로 변경 (DB 부하 80% 감소)
        private const val PROCESS_INTERVAL_MS = 500L
        private const val MAX_BATCH_SIZE = 50
    }

    /**
     * 발급 대기열 처리 메인 루프 (500ms 주기)
     *
     * 활성 쿠폰들의 대기열을 순회하며 배치 처리.
     */
    @Scheduled(fixedDelay = PROCESS_INTERVAL_MS)
    fun processPendingIssues() {
        try {
            val activeCoupons = couponDomainService.getAvailableCoupons()

            activeCoupons.forEach { coupon ->
                processCouponIssuesBatch(coupon)
            }
        } catch (e: Exception) {
            logger.error("발급 대기열 처리 중 예상치 못한 오류", e)
        }
    }

    /**
     * 특정 쿠폰의 발급 대기열 배치 처리
     *
     * ZPOPMIN으로 배치 크기만큼 유저를 꺼내어 Bulk 발급 처리.
     * 단건 insert → Bulk insert로 성능 개선.
     */
    @Transactional
    fun processCouponIssuesBatch(coupon: Coupon) {
        // ZPOPMIN으로 대기열에서 유저들을 꺼냄
        val userIds = couponIssueService.popPendingUsers(coupon.id, MAX_BATCH_SIZE)

        if (userIds.isEmpty()) {
            return
        }

        try {
            // Bulk 쿠폰 발급 (단건 → 배치)
            val userCoupons = couponDomainService.issueCouponsBatch(coupon, userIds)

            // Bulk 발급 이력 저장 (단건 → 배치)
            couponIssueHistoryService.recordIssuesBatch(
                couponId = coupon.id,
                userIds = userIds,
                couponName = coupon.name
            )

            logger.info(
                "쿠폰 배치 발급 완료 - couponId: {}, couponName: {}, count: {}",
                coupon.id, coupon.name, userCoupons.size
            )
        } catch (e: Exception) {
            // 배치 실패 시 개별 유저 로깅
            logger.error(
                "쿠폰 배치 발급 실패 - couponId: {}, userIds: {}, error: {}",
                coupon.id, userIds, e.message, e
            )
            // TODO: Phase 2에서 DLQ로 이동 처리 추가
        }
    }
}
