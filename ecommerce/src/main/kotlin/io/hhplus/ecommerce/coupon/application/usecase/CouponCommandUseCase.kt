package io.hhplus.ecommerce.coupon.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.coupon.application.CouponIssueHistoryService
import io.hhplus.ecommerce.coupon.application.CouponIssueService
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.presentation.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.presentation.dto.UseCouponRequest
import org.springframework.stereotype.Component

/**
 * 쿠폰 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 트랜잭션 경계 관리
 * - 쿠폰 발급/사용 작업 오케스트레이션
 *
 * 동시성 제어:
 * - 선착순 발급: CouponIssuePort의 SADD 원자성 활용
 * - 쿠폰 사용: @DistributedTransaction
 */
@Component
class CouponCommandUseCase(
    private val couponDomainService: CouponDomainService,
    private val couponIssueService: CouponIssueService,
    private val couponIssueHistoryService: CouponIssueHistoryService
) {

    /**
     * 사용자의 쿠폰 발급 요청을 등록한다
     *
     * 캐시 전략:
     * - 유효기간 검증: CouponCacheInfo (로컬 캐시)
     * - 수량 제어: Redis maxQuantity (동적 데이터)
     *
     * 동시성 제어는 CouponIssuePort의 SADD 원자성으로 처리.
     */
    fun issueCoupon(userId: Long, request: IssueCouponRequest) {
        // 1. 캐시된 정보로 유효기간 검증 (빠른 실패)
        val couponInfo = couponDomainService.getCouponCacheInfo(request.couponId)
            ?: throw CouponException.CouponNotFound(request.couponId)

        if (!couponInfo.isValid()) {
            throw CouponException.CouponNotAvailable(couponInfo.name)
        }

        // 2. Redis 기반 발급 요청 (수량은 Redis에서 관리)
        couponIssueService.requestIssue(userId, couponInfo)
    }

    /**
     * 주문에 쿠폰을 실제로 적용하고 할인 금액을 계산한다
     */
    @DistributedTransaction
    fun applyCoupon(userId: Long, request: UseCouponRequest, orderId: Long): Long {
        val userCoupon = couponDomainService.getUserCouponOrThrow(request.userCouponId, userId)
        val coupon = couponDomainService.getCouponOrThrow(userCoupon.couponId)

        val discountAmount = couponDomainService.applyCoupon(
            userCoupon = userCoupon,
            coupon = coupon,
            orderId = orderId,
            orderAmount = request.orderAmount
        )

        // 쿠폰 사용 이력 저장
        couponIssueHistoryService.recordUsage(
            couponId = coupon.id,
            userId = userId,
            couponName = coupon.name,
            orderId = orderId,
            issuedAt = userCoupon.issuedAt
        )

        return discountAmount
    }

    /**
     * 쿠폰 사용 가능성을 검증하고 예상 할인 금액을 계산한다
     *
     * 캐시된 CouponCacheInfo 사용 (totalQuantity 불필요).
     */
    fun validateCoupon(userId: Long, request: UseCouponRequest): Long {
        val userCoupon = couponDomainService.getUserCouponOrThrow(request.userCouponId, userId)
        val couponInfo = couponDomainService.getCouponCacheInfoOrThrow(userCoupon.couponId)

        // 사용 가능 여부 검증
        if (!userCoupon.isUsable()) {
            throw CouponException.AlreadyUsedCoupon(userCoupon.id)
        }

        // 최소 주문 금액 검증
        if (!couponInfo.isValidForUse(request.orderAmount)) {
            throw CouponException.MinimumOrderAmountNotMet(
                couponInfo.name,
                couponInfo.minimumOrderAmount,
                request.orderAmount
            )
        }

        return couponInfo.calculateDiscountAmount(request.orderAmount)
    }

    /**
     * 특정 쿠폰의 현재 대기열 크기를 조회한다
     */
    fun getPendingCount(couponId: Long): Long {
        return couponIssueService.getPendingCount(couponId)
    }

    /**
     * 특정 쿠폰의 현재 발급된 수량을 조회한다
     */
    fun getIssuedCount(couponId: Long): Long {
        return couponIssueService.getIssuedCount(couponId)
    }
}
