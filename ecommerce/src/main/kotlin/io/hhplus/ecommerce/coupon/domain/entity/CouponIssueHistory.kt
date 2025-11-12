package io.hhplus.ecommerce.coupon.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "coupon_issue_history")
class CouponIssueHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val couponId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: UserCouponStatus,

    @Column(nullable = false)
    val issuedAt: LocalDateTime,

    @Column(nullable = true)
    val usedAt: LocalDateTime? = null,

    @Column(nullable = true)
    val expiredAt: LocalDateTime? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val description: String? = null
) : ActiveJpaEntity() {

    fun isUsed(): Boolean = status == UserCouponStatus.USED

    fun isExpired(): Boolean = status == UserCouponStatus.EXPIRED

    fun isAvailable(): Boolean = status == UserCouponStatus.ISSUED

    companion object {
        fun createIssueHistory(
            couponId: Long,
            userId: Long,
            issuedAt: LocalDateTime,
            description: String? = null,
            createdBy: Long
        ): CouponIssueHistory {
            return CouponIssueHistory(
                couponId = couponId,
                userId = userId,
                status = UserCouponStatus.ISSUED,
                issuedAt = issuedAt,
                description = description
            )
        }

        fun createUsageHistory(
            couponId: Long,
            userId: Long,
            issuedAt: LocalDateTime,
            usedAt: LocalDateTime,
            description: String? = null,
            createdBy: Long
        ): CouponIssueHistory {
            return CouponIssueHistory(
                couponId = couponId,
                userId = userId,
                status = UserCouponStatus.USED,
                issuedAt = issuedAt,
                usedAt = usedAt,
                description = description
            )
        }

        fun createExpirationHistory(
            couponId: Long,
            userId: Long,
            issuedAt: LocalDateTime,
            expiredAt: LocalDateTime,
            description: String? = null,
            createdBy: Long
        ): CouponIssueHistory {
            return CouponIssueHistory(
                couponId = couponId,
                userId = userId,
                status = UserCouponStatus.EXPIRED,
                issuedAt = issuedAt,
                expiredAt = expiredAt,
                description = description
            )
        }
    }
}