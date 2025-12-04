package io.hhplus.ecommerce.coupon.application.usecase

import io.hhplus.ecommerce.coupon.application.CouponIssueHistoryService
import io.hhplus.ecommerce.coupon.application.CouponIssueService
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import org.springframework.stereotype.Component

/**
 * 쿠폰 조회 UseCase - 애플리케이션 계층 (Query)
 *
 * 역할:
 * - 쿠폰 관련 조회 작업 처리
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 쿠폰 정보 조회
 * - 사용자 쿠폰 조회
 * - 발급 상태 조회
 * - 쿠폰 이력/통계 조회
 */
@Component
class GetCouponQueryUseCase(
    private val couponDomainService: CouponDomainService,
    private val couponIssueService: CouponIssueService,
    private val couponIssueHistoryService: CouponIssueHistoryService
) {

    /**
     * 쿠폰 정보 조회
     */
    fun getCoupon(couponId: Long): Coupon? {
        return couponDomainService.getCoupon(couponId)
    }

    /**
     * 현재 발급 가능한 쿠폰 목록을 조회한다
     */
    fun getAvailableCoupons(): List<Coupon> {
        return couponDomainService.getAvailableCoupons()
    }

    /**
     * 사용자 쿠폰 조회
     */
    fun getUserCoupons(userId: Long, onlyAvailable: Boolean = false): List<UserCoupon> {
        return if (onlyAvailable) {
            couponDomainService.getUserCoupons(userId, UserCouponStatus.ISSUED)
        } else {
            couponDomainService.getUserCoupons(userId)
        }
    }

    /**
     * 사용 가능한 사용자 쿠폰 조회 (ISSUED 상태만)
     */
    fun getAvailableUserCoupons(userId: Long): List<UserCoupon> {
        return couponDomainService.getAvailableUserCoupons(userId)
    }

    // ========== 발급 상태 조회 기능 ==========

    /**
     * 특정 유저가 해당 쿠폰 발급을 이미 요청했는지 확인
     */
    fun isUserRequested(couponId: Long, userId: Long): Boolean {
        return couponIssueService.isAlreadyIssued(couponId, userId)
    }

    /**
     * 현재 발급된 수량 조회
     */
    fun getIssuedCount(couponId: Long): Long {
        return couponIssueService.getIssuedCount(couponId)
    }

    /**
     * 대기열 크기 조회
     */
    fun getPendingCount(couponId: Long): Long {
        return couponIssueService.getPendingCount(couponId)
    }

    // ========== 이력/통계 조회 기능 ==========

    /**
     * 쿠폰 발급 이력 조회
     */
    fun getCouponIssueHistory(userId: Long): List<CouponIssueHistory> {
        return couponIssueHistoryService.getUserCouponHistory(userId)
    }

    /**
     * 쿠폰 발급 통계 조회
     */
    fun getCouponIssueStatistics(couponId: Long): CouponIssueHistoryService.CouponStatistics {
        return couponIssueHistoryService.getCouponStatistics(couponId)
    }
}
