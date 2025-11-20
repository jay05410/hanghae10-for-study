package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.repository.CouponIssueHistoryRepository
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 쿠폰 발급 이력 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 쿠폰 발급 및 사용 이력 관리
 * - 쿠폰 생명주기 추적 및 기록
 * - 쿠폰 통계 데이터 제공
 *
 * 책임:
 * - 쿠폰 발급, 사용, 만료 이력 기록
 * - 사용자 및 쿠폰별 이력 조회
 * - 쿠폰 사용률 및 통계 계산
 */
@Service
class CouponIssueHistoryService(
    private val couponIssueHistoryRepository: CouponIssueHistoryRepository
) {

    @Transactional
    fun recordIssue(
        couponId: Long,
        userId: Long,
        couponName: String
    ): CouponIssueHistory {
        val issueHistory = CouponIssueHistory.createIssueHistory(
            couponId = couponId,
            userId = userId,
            issuedAt = LocalDateTime.now(),
            description = "쿠폰 발급: $couponName"
        )
        return couponIssueHistoryRepository.save(issueHistory)
    }

    @Transactional
    fun recordUsage(
        couponId: Long,
        userId: Long,
        couponName: String,
        orderId: Long,
        issuedAt: LocalDateTime
    ): CouponIssueHistory {
        val usageHistory = CouponIssueHistory.createUsageHistory(
            couponId = couponId,
            userId = userId,
            issuedAt = issuedAt,
            usedAt = LocalDateTime.now(),
            description = "쿠폰 사용: $couponName, 주문 ID: $orderId"
        )
        return couponIssueHistoryRepository.save(usageHistory)
    }

    @Transactional
    fun recordExpiration(
        couponId: Long,
        userId: Long,
        couponName: String,
        issuedAt: LocalDateTime
    ): CouponIssueHistory {
        val expirationHistory = CouponIssueHistory.createExpirationHistory(
            couponId = couponId,
            userId = userId,
            issuedAt = issuedAt,
            expiredAt = LocalDateTime.now(),
            description = "쿠폰 만료: $couponName"
        )
        return couponIssueHistoryRepository.save(expirationHistory)
    }

    fun getUserCouponHistory(userId: Long): List<CouponIssueHistory> {
        return couponIssueHistoryRepository.findByUserId(userId)
    }

    fun getCouponHistory(couponId: Long): List<CouponIssueHistory> {
        return couponIssueHistoryRepository.findByCouponId(couponId)
    }

    fun getCouponStatistics(couponId: Long): CouponStatistics {
        val issuedCount = couponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.ISSUED)
        val usedCount = couponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.USED)
        val expiredCount = couponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.EXPIRED)

        return CouponStatistics(
            couponId = couponId,
            totalIssued = issuedCount,
            totalUsed = usedCount,
            totalExpired = expiredCount,
            usageRate = if (issuedCount > 0) (usedCount.toDouble() / issuedCount.toDouble() * 100) else 0.0
        )
    }

    fun getRecentIssueHistory(limit: Int = 100): List<CouponIssueHistory> {
        return couponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED)
            .sortedByDescending { it.issuedAt }
            .take(limit)
    }

    data class CouponStatistics(
        val couponId: Long,
        val totalIssued: Long,
        val totalUsed: Long,
        val totalExpired: Long,
        val usageRate: Double
    )
}