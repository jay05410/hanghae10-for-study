package io.hhplus.ecommerce.coupon.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.common.exception.coupon.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
// import jakarta.persistence.*
import java.time.LocalDateTime

// @Entity
// @Table(name = "user_coupons")
class UserCoupon(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val userId: Long,

    // @Column(nullable = false)
    val couponId: Long,

    // @Column(nullable = false)
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    // @Column
    var usedAt: LocalDateTime? = null,

    // @Column
    val usedOrderId: Long? = null,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    var status: UserCouponStatus = UserCouponStatus.ISSUED
) : ActiveJpaEntity() {
    fun isUsable(): Boolean = status == UserCouponStatus.ISSUED && isActive

    fun use(orderId: Long, usedBy: Long) {
        if (!isUsable()) {
            throw CouponException.AlreadyUsedCoupon(id)
        }

        this.status = UserCouponStatus.USED
        this.usedAt = LocalDateTime.now()
    }

    fun isExpired(couponValidTo: LocalDateTime): Boolean {
        return LocalDateTime.now().isAfter(couponValidTo)
    }

    fun expire(expiredBy: Long) {
        if (status == UserCouponStatus.USED) {
            throw IllegalStateException("이미 사용된 쿠폰은 만료 처리할 수 없습니다")
        }

        this.status = UserCouponStatus.EXPIRED
    }

    companion object {
        fun create(
            userId: Long,
            couponId: Long,
            createdBy: Long
        ): UserCoupon {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(couponId > 0) { "쿠폰 ID는 유효해야 합니다" }

            return UserCoupon(
                userId = userId,
                couponId = couponId
            )
        }
    }
}