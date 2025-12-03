package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.common.util.RedisUtil
import io.hhplus.ecommerce.coupon.application.CouponQueueService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 쿠폰 UseCase
 *
 * 역할:
 * - 쿠폰 발급 Queue 등록 및 조회
 * - 쿠폰 사용 기능 제공
 * - Queue 상태 관리
 *
 * 책임:
 * - 쿠폰 발급 Queue 등록 (직접 발급 대신)
 * - 쿠폰 사용 요청 검증 및 실행
 * - Queue 요청 상태 조회
 * - 대기열 관리
 */
@Component
class CouponUseCase(
    private val couponService: CouponService,
    private val couponQueueService: CouponQueueService,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    /**
     * 사용자의 쿠폰 발급 요청을 Queue에 등록한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 발급 요청 데이터
     * @return Queue에 등록된 요청 정보 (대기 순번, 예상 시간 포함)
     * @throws CouponException.CouponNotFound 쿠폰을 찾을 수 없는 경우
     * @throws CouponException.AlreadyInQueue 이미 Queue에 등록된 경우
     * @throws CouponException.CouponNotAvailable 쿠폰 발급이 불가능한 경우
     * @throws CouponException.QueueFull 대기열이 가득 찬 경우
     */
    @DistributedLock(key = DistributedLockKeys.Coupon.ISSUE)
    fun issueCoupon(userId: Long, request: IssueCouponRequest): CouponQueueRequest {
        val coupon = couponService.getCoupon(request.couponId)
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
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 사용 요청 데이터
     * @param orderId 쿠폰이 적용될 주문 ID
     * @return 계산된 할인 금액
     * @throws IllegalArgumentException 쿠폰 사용 조건을 만족하지 않는 경우
     * @throws RuntimeException 쿠폰 사용 처리에 실패한 경우
     */
    fun applyCoupon(userId: Long, request: UseCouponRequest, orderId: Long): Long {
        return couponService.applyCoupon(
            userId = userId,
            userCouponId = request.userCouponId,
            orderId = orderId,
            orderAmount = request.orderAmount
        )
    }

    // ========== Queue 조회 기능 ==========

    /**
     * Queue ID로 요청 상태를 조회한다
     *
     * @param queueId Queue ID
     * @return Queue 요청 정보 (없으면 null)
     */
    fun getQueueStatus(queueId: String): CouponQueueRequest? {
        return couponQueueService.getQueueRequest(queueId)
    }

    /**
     * 사용자의 특정 쿠폰에 대한 Queue 요청을 조회한다
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return Queue 요청 정보 (없으면 null)
     */
    fun getUserQueueRequest(userId: Long, couponId: Long): CouponQueueRequest? {
        return couponQueueService.getUserQueueRequest(userId, couponId)
    }

    /**
     * 특정 쿠폰의 현재 대기열 크기를 조회한다
     *
     * @param couponId 쿠폰 ID
     * @return 대기 중인 요청 수
     */
    fun getQueueSize(couponId: Long): Long {
        return RedisUtil.getQueueSize(redisTemplate, RedisKeyNames.CouponQueue.waitingKey(couponId))
    }

    // ========== 쿠폰 조회 기능 ==========

    /**
     * 현재 발급 가능한 쿠폰 목록을 조회한다
     *
     * @return 발급 가능한 쿠폰 목록
     */
    fun getAvailableCoupons(): List<Coupon> {
        return couponService.getAvailableCoupons()
    }

    /**
     * 사용자 쿠폰 조회
     *
     * @param userId 사용자 ID
     * @param onlyAvailable true면 사용 가능한 쿠폰만, false면 전체
     */
    fun getUserCoupons(userId: Long, onlyAvailable: Boolean = false): List<UserCoupon> {
        return if (onlyAvailable) {
            couponService.getUserCoupons(userId, io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus.ISSUED)
        } else {
            couponService.getUserCoupons(userId)
        }
    }

    /**
     * 사용 가능한 사용자 쿠폰 조회 (ISSUED 상태만)
     *
     * @param userId 사용자 ID
     * @return 사용 가능한 쿠폰 목록
     */
    fun getAvailableUserCoupons(userId: Long): List<UserCoupon> {
        return couponService.getAvailableUserCoupons(userId)
    }

    /**
     * 쿠폰 사용 가능성을 검증하고 예상 할인 금액을 계산한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 사용 검증 요청 데이터
     * @return 검증 성공 시 계산된 할인 금액
     * @throws IllegalArgumentException 쿠폰 사용 조건을 만족하지 않는 경우
     */
    fun validateCoupon(userId: Long, request: UseCouponRequest): Long {
        return couponService.validateCouponUsage(
            userId = userId,
            userCouponId = request.userCouponId,
            orderAmount = request.orderAmount
        )
    }
}