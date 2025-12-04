package io.hhplus.ecommerce.coupon.infra.persistence.adapter

import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.repository.CouponIssueHistoryRepository
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.infra.persistence.repository.CouponIssueHistoryJpaRepository
import org.springframework.stereotype.Repository

/**
 * CouponIssueHistory Repository JPA 구현체
 */
@Repository
class CouponIssueHistoryRepositoryImpl(
    private val jpaRepository: CouponIssueHistoryJpaRepository
) : CouponIssueHistoryRepository {

    override fun save(couponIssueHistory: CouponIssueHistory): CouponIssueHistory =
        jpaRepository.save(couponIssueHistory)

    override fun findById(id: Long): CouponIssueHistory? =
        jpaRepository.findById(id).orElse(null)

    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): List<CouponIssueHistory> =
        jpaRepository.findByCouponIdAndUserId(couponId, userId)

    override fun findByUserId(userId: Long): List<CouponIssueHistory> =
        jpaRepository.findByUserId(userId)

    override fun findByCouponId(couponId: Long): List<CouponIssueHistory> =
        jpaRepository.findByCouponId(couponId)

    override fun findByStatus(status: UserCouponStatus): List<CouponIssueHistory> =
        jpaRepository.findByStatus(status)

    override fun countByCouponIdAndStatus(couponId: Long, status: UserCouponStatus): Long =
        jpaRepository.countByCouponIdAndStatus(couponId, status)
}