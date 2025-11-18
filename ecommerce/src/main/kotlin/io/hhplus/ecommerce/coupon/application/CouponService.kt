package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
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
     * 발급 가능한 쿼폰 목록을 조회한다
     *
     * @return 발급 가능한 쿼폰 목록
     */
    fun getAvailableCoupons(): List<Coupon> {
        return couponRepository.findAvailableCoupons()
            .filter { it.isAvailableForIssue() }
    }

    /**
     * 쿼폰명으로 쿼폰을 조회한다
     *
     * @param name 조회할 쿼폰명
     * @return 쿼폰 엔티티 (존재하지 않을 경우 null)
     */
    fun getCouponByName(name: String): Coupon? {
        return couponRepository.findByName(name)
    }

    /**
     * 사용자에게 쿼폰을 발급한다
     *
     * @param userId 쿼폰을 발급받을 사용자 ID
     * @param couponId 발급할 쿼폰 ID
     * @return 발급된 사용자 쿼폰
     * @throws CouponException.CouponNotFound 쿼폰을 찾을 수 없는 경우
     * @throws CouponException.CouponSoldOut 쿼폰이 품절된 경우
     * @throws CouponException.AlreadyIssuedCoupon 이미 발급받은 쿼폰인 경우
     */
    @Transactional
    fun issueCoupon(userId: Long, couponId: Long): UserCoupon {
        val coupon = couponRepository.findByIdWithLock(couponId)
            ?: throw CouponException.CouponNotFound(couponId)

        if (!coupon.isAvailableForIssue()) {
            throw CouponException.CouponSoldOut(coupon.name, coupon.getRemainingQuantity())
        }

        val existingUserCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
        if (existingUserCoupon != null) {
            throw CouponException.AlreadyIssuedCoupon(userId, coupon.name)
        }

        // 가변 모델이므로 issue() 메서드 호출 후 저장
        coupon.issue(userId)
        couponRepository.save(coupon)

        val userCoupon = UserCoupon.create(
            userId = userId,
            couponId = coupon.id,
            createdBy = userId
        )

        val savedUserCoupon = userCouponRepository.save(userCoupon)

        // 쿠폰 발급 이력 저장
        couponIssueHistoryService.recordIssue(
            couponId = coupon.id,
            userId = userId,
            couponName = coupon.name
        )

        return savedUserCoupon
    }

    fun getUserCoupons(userId: Long): List<UserCoupon> {
        return userCouponRepository.findByUserId(userId)
    }

    fun getAvailableUserCoupons(userId: Long): List<UserCoupon> {
        return userCouponRepository.findByUserIdAndStatus(userId, UserCouponStatus.ISSUED)
            .filter { it.isUsable() }
    }

    /**
     * 쿼폰을 사용하여 할인을 적용한다
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용할 사용자 쿼폰 ID
     * @param orderId 주문 ID
     * @param orderAmount 주문 금액
     * @return 할인 금액
     * @throws CouponException.UserCouponNotFound 사용자 쿼폰을 찾을 수 없는 경우
     * @throws CouponException.AlreadyUsedCoupon 이미 사용된 쿼폰인 경우
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
        userCoupon.use(orderId, userId)
        userCouponRepository.save(userCoupon)

        // 쿠폰 사용 이력 저장
        couponIssueHistoryService.recordUsage(
            couponId = coupon.id,
            userId = userId,
            couponName = coupon.name,
            orderId = orderId,
            issuedAt = userCoupon.createdAt
        )

        return discountAmount
    }

    /**
     * 쿼폰 사용 가능 여부를 검증하고 할인 금액을 계산한다
     *
     * @param userId 사용자 ID
     * @param userCouponId 검증할 사용자 쿜폰 ID
     * @param orderAmount 주문 금액
     * @return 예상 할인 금액
     * @throws CouponException.UserCouponNotFound 사용자 쿼폰을 찾을 수 없는 경우
     * @throws CouponException.AlreadyUsedCoupon 이미 사용된 쿼폰인 경우
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