package io.hhplus.ecommerce.coupon.domain.entity

import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import java.time.LocalDateTime

/**
 * 사용자 쿠폰 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 사용자에게 발급된 쿠폰 정보 관리
 * - 쿠폰 사용 및 만료 처리
 * - 쿠폰 사용 가능 여부 검증
 *
 * 비즈니스 규칙:
 * - 발급된 쿠폰만 사용 가능
 * - 사용된 쿠폰은 다시 사용할 수 없음
 * - 사용된 쿠폰은 만료 처리할 수 없음
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/UserCouponJpaEntity에서 처리됩니다.
 */
data class UserCoupon(
    val id: Long = 0,
    val userId: Long,
    val couponId: Long,
    val issuedAt: LocalDateTime = LocalDateTime.now(),
    var usedAt: LocalDateTime? = null,
    var usedOrderId: Long? = null,
    var status: UserCouponStatus = UserCouponStatus.ISSUED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0
) {
    fun isUsable(): Boolean = status == UserCouponStatus.ISSUED

    /**
     * 쿠폰을 사용하고 사용 정보 업데이트
     *
     * @param orderId 주문 ID
     * @param usedBy 사용자 ID
     * @throws CouponException.AlreadyUsedCoupon 이미 사용된 쿠폰인 경우
     */
    fun use(orderId: Long, usedBy: Long) {
        if (!isUsable()) {
            throw CouponException.AlreadyUsedCoupon(id)
        }

        val now = LocalDateTime.now()
        this.status = UserCouponStatus.USED
        this.usedAt = now
        this.usedOrderId = orderId
        this.updatedBy = usedBy
        this.updatedAt = now
    }

    fun isExpired(couponValidTo: LocalDateTime): Boolean {
        return LocalDateTime.now().isAfter(couponValidTo)
    }

    /**
     * 쿠폰을 만료 처리
     *
     * @param expiredBy 만료 처리자 ID
     * @throws IllegalStateException 이미 사용된 쿠폰인 경우
     */
    fun expire(expiredBy: Long) {
        if (status == UserCouponStatus.USED) {
            throw IllegalStateException("이미 사용된 쿠폰은 만료 처리할 수 없습니다")
        }

        this.status = UserCouponStatus.EXPIRED
        this.updatedBy = expiredBy
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            userId: Long,
            couponId: Long,
            createdBy: Long
        ): UserCoupon {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(couponId > 0) { "쿠폰 ID는 유효해야 합니다" }

            val now = LocalDateTime.now()
            return UserCoupon(
                userId = userId,
                couponId = couponId,
                issuedAt = now,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}