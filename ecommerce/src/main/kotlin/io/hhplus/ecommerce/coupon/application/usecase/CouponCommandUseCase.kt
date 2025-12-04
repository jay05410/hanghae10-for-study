package io.hhplus.ecommerce.coupon.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.common.util.RedisUtil
import io.hhplus.ecommerce.coupon.application.CouponIssueHistoryService
import io.hhplus.ecommerce.coupon.application.CouponQueueService
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.presentation.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.presentation.dto.UseCouponRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 쿠폰 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 트랜잭션 경계 관리
 * - 분산락을 통한 동시성 제어
 * - 쿠폰 발급/사용 작업 오케스트레이션
 *
 * 책임:
 * - 쿠폰 발급 Queue 등록
 * - 쿠폰 사용 요청 검증 및 실행
 * - Queue 요청 상태 관리
 */
@Component
class CouponCommandUseCase(
    private val couponDomainService: CouponDomainService,
    private val couponQueueService: CouponQueueService,
    private val couponIssueHistoryService: CouponIssueHistoryService,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    /**
     * 사용자의 쿠폰 발급 요청을 Queue에 등록한다
     */
    @DistributedLock(key = DistributedLockKeys.Coupon.ISSUE)
    fun issueCoupon(userId: Long, request: IssueCouponRequest): CouponQueueRequest {
        val coupon = couponDomainService.getCoupon(request.couponId)
            ?: throw CouponException.CouponNotFound(request.couponId)

        // 비즈니스 검증: 쿠폰 발급 가능 여부
        if (!coupon.isAvailableForIssue()) {
            throw CouponException.CouponNotAvailable(coupon.name)
        }

        // 큐 크기 제한과 함께 enqueue를 원자적으로 실행
        return couponQueueService.enqueueWithSizeLimit(
            userId = userId,
            coupon = coupon
        )
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
     */
    fun validateCoupon(userId: Long, request: UseCouponRequest): Long {
        val userCoupon = couponDomainService.getUserCouponOrThrow(request.userCouponId, userId)
        val coupon = couponDomainService.getCouponOrThrow(userCoupon.couponId)

        return couponDomainService.validateCouponUsage(
            userCoupon = userCoupon,
            coupon = coupon,
            orderAmount = request.orderAmount
        )
    }

    /**
     * 특정 쿠폰의 현재 대기열 크기를 조회한다
     */
    fun getQueueSize(couponId: Long): Long {
        return RedisUtil.getQueueSize(redisTemplate, RedisKeyNames.CouponQueue.waitingKey(couponId))
    }
}
