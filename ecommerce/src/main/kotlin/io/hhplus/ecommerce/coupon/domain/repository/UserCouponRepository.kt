package io.hhplus.ecommerce.coupon.domain.repository

import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon

interface UserCouponRepository {
    fun save(userCoupon: UserCoupon): UserCoupon
    fun findById(id: Long): UserCoupon?
    fun findByUserId(userId: Long): List<UserCoupon>
    fun findByUserIdAndStatus(userId: Long, status: UserCouponStatus): List<UserCoupon>
    fun findByCouponId(couponId: Long): List<UserCoupon>
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon?
    fun findByUserIdAndCouponCode(userId: Long, couponCode: String): UserCoupon?
    fun countByUserIdAndCouponId(userId: Long, couponId: Long): Long
}