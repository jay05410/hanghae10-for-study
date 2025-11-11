package io.hhplus.ecommerce.coupon.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.common.exception.coupon.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
// import jakarta.persistence.*
import java.time.LocalDateTime

// @Entity
// @Table(name = "coupons")
class Coupon(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true, length = 50)
    val name: String,

    // @Column(nullable = false, unique = true, length = 20)
    val code: String,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    val discountType: DiscountType,

    // @Column(nullable = false)
    val discountValue: Long,

    // @Column(nullable = false)
    val minimumOrderAmount: Long = 0,

    // @Column(nullable = false)
    val totalQuantity: Int,

    // @Column(nullable = false)
    var issuedQuantity: Int = 0,

    // @Version
    var version: Int = 0,

    // @Column(nullable = false)
    val validFrom: LocalDateTime,

    // @Column(nullable = false)
    val validTo: LocalDateTime
) : ActiveJpaEntity() {
    fun getRemainingQuantity(): Int = totalQuantity - issuedQuantity

    fun isAvailableForIssue(): Boolean =
        isActive && getRemainingQuantity() > 0 && isWithinValidPeriod()

    fun isValidForUse(orderAmount: Long): Boolean =
        isActive && isWithinValidPeriod() && orderAmount >= minimumOrderAmount

    fun isWithinValidPeriod(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(validFrom) && now.isBefore(validTo)
    }

    fun issue(issuedBy: Long): Int {
        if (!isAvailableForIssue()) {
            throw CouponException.CouponSoldOut(name, getRemainingQuantity())
        }

        val oldIssuedQuantity = this.issuedQuantity
        this.issuedQuantity += 1

        return oldIssuedQuantity
    }

    fun calculateDiscountAmount(orderAmount: Long): Long {
        if (!isValidForUse(orderAmount)) {
            return 0L
        }

        return when (discountType) {
            DiscountType.FIXED -> minOf(discountValue, orderAmount)
            DiscountType.PERCENTAGE -> (orderAmount * discountValue / 100)
                .coerceAtMost(orderAmount)
        }
    }

    companion object {
        fun create(
            name: String,
            code: String,
            discountType: DiscountType,
            discountValue: Long,
            minimumOrderAmount: Long = 0,
            totalQuantity: Int,
            validFrom: LocalDateTime,
            validTo: LocalDateTime,
            createdBy: Long
        ): Coupon {
            require(name.isNotBlank()) { "쿠폰명은 필수입니다" }
            require(discountValue > 0) { "할인값은 0보다 커야 합니다" }
            require(minimumOrderAmount >= 0) { "최소 주문 금액은 0 이상이어야 합니다" }
            require(totalQuantity > 0) { "총 발행량은 0보다 커야 합니다" }
            require(validTo.isAfter(validFrom)) { "유효 종료일은 시작일보다 늦어야 합니다" }

            if (discountType == DiscountType.PERCENTAGE) {
                require(discountValue <= 100) { "퍼센트 할인값은 100 이하여야 합니다" }
            }

            return Coupon(
                name = name,
                code = code,
                discountType = discountType,
                discountValue = discountValue,
                minimumOrderAmount = minimumOrderAmount,
                totalQuantity = totalQuantity,
                validFrom = validFrom,
                validTo = validTo
            )
        }
    }
}

