package io.hhplus.ecommerce.coupon.domain.repository

import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus

interface CouponIssueHistoryRepository {
    fun save(couponIssueHistory: CouponIssueHistory): CouponIssueHistory
    fun saveAll(histories: List<CouponIssueHistory>): List<CouponIssueHistory>
    fun findById(id: Long): CouponIssueHistory?
    fun findByCouponIdAndUserId(couponId: Long, userId: Long): List<CouponIssueHistory>
    fun findByUserId(userId: Long): List<CouponIssueHistory>
    fun findByCouponId(couponId: Long): List<CouponIssueHistory>
    fun findByStatus(status: UserCouponStatus): List<CouponIssueHistory>
    fun countByCouponIdAndStatus(couponId: Long, status: UserCouponStatus): Long
}