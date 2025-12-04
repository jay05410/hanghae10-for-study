package io.hhplus.ecommerce.coupon.domain.service

import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.exception.CouponException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/**
 * 쿠폰 도메인 서비스 - 도메인 계층
 *
 * 역할:
 * - 쿠폰 도메인의 핵심 비즈니스 로직 처리
 * - 쿠폰 발급 및 사용 프로세스 관리
 * - 사용자 쿠폰 생명주기 관리
 *
 * 책임:
 * - 쿠폰 조회 및 발급 가능 여부 검증
 * - 사용자 쿠폰 발급 및 사용 처리
 * - 쿠폰 사용 검증 및 할인 금액 계산
 *
 * 주의:
 * - @Transactional 사용 금지 (UseCase에서 관리)
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class CouponDomainService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) {

    /**
     * 쿠폰 정보 조회 (캐시 적용)
     */
    @Cacheable(value = [CacheNames.COUPON_INFO], key = "#couponId")
    fun getCoupon(couponId: Long): Coupon? {
        return couponRepository.findById(couponId)
    }

    /**
     * 쿠폰 정보 조회 (없으면 예외)
     */
    fun getCouponOrThrow(couponId: Long): Coupon {
        return couponRepository.findById(couponId)
            ?: throw CouponException.CouponNotFound(couponId)
    }


    /**
     * 발급 가능한 쿠폰 목록 조회 (재고 있고 유효기간 내)
     */
    fun getAvailableCoupons(): List<Coupon> {
        return couponRepository.findAll()
            .filter { it.isAvailableForIssue() }
    }

    /**
     * 사용자 쿠폰 발급
     *
     * 주의: 이 메서드는 모든 검증이 완료된 후 호출되어야 함
     *      (발급 가능 여부, 재고, 중복 발급 등)
     */
    fun issueCoupon(coupon: Coupon, userId: Long): UserCoupon {
        // 방어적 검증: 중복 발급 확인
        val existingUserCoupon = userCouponRepository.findByUserIdAndCouponId(userId, coupon.id)
        if (existingUserCoupon != null) {
            throw CouponException.AlreadyIssuedCoupon(userId, coupon.name)
        }

        // 쿠폰 재고 차감
        coupon.issue()
        couponRepository.save(coupon)

        // 사용자 쿠폰 생성
        val userCoupon = UserCoupon.create(
            userId = userId,
            couponId = coupon.id
        )

        return userCouponRepository.save(userCoupon)
    }

    /**
     * 사용자 쿠폰 조회
     */
    fun getUserCoupons(userId: Long, status: UserCouponStatus? = null): List<UserCoupon> {
        return if (status != null) {
            userCouponRepository.findByUserIdAndStatus(userId, status)
        } else {
            userCouponRepository.findByUserId(userId)
        }
    }

    /**
     * 사용 가능한 사용자 쿠폰 조회 (ISSUED 상태만)
     */
    fun getAvailableUserCoupons(userId: Long): List<UserCoupon> {
        return getUserCoupons(userId, UserCouponStatus.ISSUED)
    }

    /**
     * 사용자 쿠폰 ID로 조회
     */
    fun getUserCoupon(userCouponId: Long): UserCoupon? {
        return userCouponRepository.findById(userCouponId)
    }

    /**
     * 사용자 쿠폰 ID로 조회 (없으면 예외)
     */
    fun getUserCouponOrThrow(userCouponId: Long, userId: Long): UserCoupon {
        return userCouponRepository.findById(userCouponId)
            ?: throw CouponException.UserCouponNotFound(userId)
    }

    /**
     * 쿠폰 사용 적용
     *
     * @return 할인 금액
     */
    fun applyCoupon(userCoupon: UserCoupon, coupon: Coupon, orderId: Long, orderAmount: Long): Long {
        // 사용 가능 여부 검증
        if (!userCoupon.isUsable()) {
            throw CouponException.AlreadyUsedCoupon(userCoupon.id)
        }

        // 최소 주문 금액 검증
        if (!coupon.isValidForUse(orderAmount)) {
            throw CouponException.MinimumOrderAmountNotMet(
                coupon.name,
                coupon.minimumOrderAmount,
                orderAmount
            )
        }

        // 할인 금액 계산
        val discountAmount = coupon.calculateDiscountAmount(orderAmount)

        // 쿠폰 사용 처리
        userCoupon.use(orderId)
        userCouponRepository.save(userCoupon)

        return discountAmount
    }

    /**
     * 쿠폰 사용 가능 여부 검증 및 할인 금액 계산
     */
    fun validateCouponUsage(userCoupon: UserCoupon, coupon: Coupon, orderAmount: Long): Long {
        if (!userCoupon.isUsable()) {
            throw CouponException.AlreadyUsedCoupon(userCoupon.id)
        }

        if (!coupon.isValidForUse(orderAmount)) {
            throw CouponException.MinimumOrderAmountNotMet(
                coupon.name,
                coupon.minimumOrderAmount,
                orderAmount
            )
        }

        return coupon.calculateDiscountAmount(orderAmount)
    }

    /**
     * 사용자의 특정 쿠폰 보유 여부 확인
     */
    fun hasUserCoupon(userId: Long, couponId: Long): Boolean {
        return userCouponRepository.findByUserIdAndCouponId(userId, couponId) != null
    }

    /**
     * 쿠폰 사용 적용 (userId, userCouponId 기반)
     *
     * 주의: 외부 도메인(Order)에서 호출하는 편의 메서드
     *
     * @return 할인 금액
     */
    fun applyCoupon(userId: Long, userCouponId: Long, orderId: Long, orderAmount: Long): Long {
        val userCoupon = getUserCouponOrThrow(userCouponId, userId)
        val coupon = getCouponOrThrow(userCoupon.couponId)
        return applyCoupon(userCoupon, coupon, orderId, orderAmount)
    }

    /**
     * 쿠폰 사용 가능 여부 검증 (userId, userCouponId 기반)
     *
     * 주의: 외부 도메인(Order)에서 호출하는 편의 메서드
     */
    fun validateCouponUsage(userId: Long, userCouponId: Long, orderAmount: Long): Long {
        val userCoupon = getUserCouponOrThrow(userCouponId, userId)
        val coupon = getCouponOrThrow(userCoupon.couponId)
        return validateCouponUsage(userCoupon, coupon, orderAmount)
    }
}
