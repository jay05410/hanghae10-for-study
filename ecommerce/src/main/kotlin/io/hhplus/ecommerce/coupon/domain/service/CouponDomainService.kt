package io.hhplus.ecommerce.coupon.domain.service

import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.domain.vo.CouponCacheInfo
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
     * 쿠폰 캐시 정보 조회 (로컬 캐시 적용)
     *
     * 정적 정보만 캐싱 (totalQuantity 제외).
     * 할인 계산, 유효성 검증 등에 사용.
     *
     * @see CouponCacheInfo 캐시용 VO
     */
    @Cacheable(value = [CacheNames.COUPON_INFO], key = "#couponId")
    fun getCouponCacheInfo(couponId: Long): CouponCacheInfo? {
        return couponRepository.findById(couponId)?.let { CouponCacheInfo.from(it) }
    }

    /**
     * 쿠폰 엔티티 조회 (캐시 미적용)
     *
     * 선착순 발급, 수량 변경 등 동적 데이터가 필요한 경우 사용.
     * totalQuantity가 필요하면 이 메서드 사용.
     */
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
     * 쿠폰 캐시 정보 조회 (없으면 예외)
     */
    fun getCouponCacheInfoOrThrow(couponId: Long): CouponCacheInfo {
        return getCouponCacheInfo(couponId)
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
     * 사용자 쿠폰 배치 발급
     *
     * Redis에서 이미 중복/재고 검증이 완료된 유저 목록을 대상으로 배치 발급.
     * DB 재고 차감은 발급 수량만큼 한 번에 처리.
     *
     * @param coupon 발급할 쿠폰
     * @param userIds 발급 대상 사용자 ID 목록
     * @return 발급된 UserCoupon 목록
     */
    fun issueCouponsBatch(coupon: Coupon, userIds: List<Long>): List<UserCoupon> {
        if (userIds.isEmpty()) return emptyList()

        // 쿠폰 재고 배치 차감 (발급 수량만큼 한 번에)
        repeat(userIds.size) { coupon.issue() }
        couponRepository.save(coupon)

        // 사용자 쿠폰 배치 생성
        val userCoupons = userIds.map { userId ->
            UserCoupon.create(
                userId = userId,
                couponId = coupon.id
            )
        }

        return userCouponRepository.saveAll(userCoupons)
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

    /**
     * 쿠폰 상태만 USED로 변경 (할인 계산 없이)
     *
     * 주문 생성 시 PricingDomainService에서 이미 할인이 계산되었으므로
     * 결제 완료 후 쿠폰 상태만 변경하면 됨.
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID
     * @param orderId 주문 ID (쿠폰 사용 추적용)
     */
    fun markCouponAsUsed(userId: Long, userCouponId: Long, orderId: Long) {
        val userCoupon = getUserCouponOrThrow(userCouponId, userId)

        if (!userCoupon.isUsable()) {
            // 이미 사용된 쿠폰은 무시 (멱등성 보장)
            return
        }

        userCoupon.use(orderId)
        userCouponRepository.save(userCoupon)
    }
}
