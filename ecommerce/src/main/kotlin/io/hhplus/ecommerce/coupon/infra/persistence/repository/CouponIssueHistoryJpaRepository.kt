package io.hhplus.ecommerce.coupon.infra.persistence.repository

import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import org.springframework.data.jpa.repository.JpaRepository

interface CouponIssueHistoryJpaRepository : JpaRepository<CouponIssueHistory, Long> {

    fun findByCouponIdAndUserId(couponId: Long, userId: Long): List<CouponIssueHistory>

    fun findByUserId(userId: Long): List<CouponIssueHistory>

    fun findByCouponId(couponId: Long): List<CouponIssueHistory>

    fun findByStatus(status: UserCouponStatus): List<CouponIssueHistory>

    fun countByCouponIdAndStatus(couponId: Long, status: UserCouponStatus): Long
}