package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 도메인 서비스 - 애플리케이션 계층
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
 * - 쿠폰 이력 관리 및 통계 제공
 */
@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val couponIssueHistoryService: CouponIssueHistoryService
) {

    /**
     * 쿠폰 정보 조회 (캐시 적용)
     *
     * 캐싱 전략:
     * - 쿠폰 기본 정보는 자주 변경되지 않으므로 캐시 적용
     * - 동시 요청 시 DB 부하 감소
     * - 캐시 키: 쿠폰 ID
     *
     * @param couponId 쿠폰 ID
     * @return 쿠폰 정보 (없으면 null)
     */
    @Cacheable(value = [CacheNames.COUPON_INFO], key = "#couponId")
    fun getCoupon(couponId: Long): Coupon? {
        return couponRepository.findById(couponId)
    }

    /**
     * 발급 가능한 쿠폰 목록 조회 (재고 있고 유효기간 내)
     */
    fun getAvailableCoupons(): List<Coupon> {
        return couponRepository.findAll()
            .filter { it.isAvailableForIssue() }
    }


    /**
     * 사용자 쿠폰을 발급한다 - 순수한 쿠폰 발급 로직
     *
     * 주의: 이 메서드는 모든 검증이 완료된 후 호출되어야 함
     *      (발급 가능 여부, 재고, 중복 발급 등)
     *
     * @param coupon 발급할 쿠폰 엔티티 (UseCase에서 조회된 것)
     * @param userId 사용자 ID
     * @return 발급된 사용자 쿠폰
     */
    @Transactional
    fun issueCoupon(coupon: Coupon, userId: Long): UserCoupon {
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
     *
     * @param userId 사용자 ID
     * @param status 쿠폰 상태 (null이면 전체 조회)
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
     *
     * @param userId 사용자 ID
     * @return 사용 가능한 쿠폰 목록
     */
    fun getAvailableUserCoupons(userId: Long): List<UserCoupon> {
        return getUserCoupons(userId, UserCouponStatus.ISSUED)
    }

    /**
     * 쿠폰을 사용하여 할인을 적용한다
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용할 사용자 쿠폰 ID
     * @param orderId 주문 ID
     * @param orderAmount 주문 금액
     * @return 할인 금액
     * @throws CouponException.UserCouponNotFound 사용자 쿠폰을 찾을 수 없는 경우
     * @throws CouponException.AlreadyUsedCoupon 이미 사용된 쿠폰인 경우
     * @throws CouponException.MinimumOrderAmountNotMet 최소 주문 금액 미달 시
     */
    @Transactional
    fun applyCoupon(userId: Long, userCouponId: Long, orderId: Long, orderAmount: Long): Long {
        val userCoupon = userCouponRepository.findById(userCouponId)
            ?: throw CouponException.UserCouponNotFound(userId)

        val coupon = couponRepository.findById(userCoupon.couponId)
            ?: throw CouponException.CouponNotFound(userCoupon.couponId)

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

        val discountAmount = coupon.calculateDiscountAmount(orderAmount)

        // 가변 모델이므로 use() 메서드 호출 후 저장
        userCoupon.use(orderId)
        userCouponRepository.save(userCoupon)

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
     * 쿠폰 사용 가능 여부를 검증하고 할인 금액을 계산한다
     *
     * @param userId 사용자 ID
     * @param userCouponId 검증할 사용자 쿜폰 ID
     * @param orderAmount 주문 금액
     * @return 예상 할인 금액
     * @throws CouponException.UserCouponNotFound 사용자 쿠폰을 찾을 수 없는 경우
     * @throws CouponException.AlreadyUsedCoupon 이미 사용된 쿠폰인 경우
     * @throws CouponException.MinimumOrderAmountNotMet 최소 주문 금액 미달 시
     */
    fun validateCouponUsage(userId: Long, userCouponId: Long, orderAmount: Long): Long {
        val userCoupon = userCouponRepository.findById(userCouponId)
            ?: throw CouponException.UserCouponNotFound(userId)

        val coupon = couponRepository.findById(userCoupon.couponId)
            ?: throw CouponException.CouponNotFound(userCoupon.couponId)

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

    fun getCouponIssueHistory(userId: Long): List<CouponIssueHistory> {
        return couponIssueHistoryService.getUserCouponHistory(userId)
    }

    fun getCouponIssueStatistics(couponId: Long): CouponIssueHistoryService.CouponStatistics {
        return couponIssueHistoryService.getCouponStatistics(couponId)
    }
}