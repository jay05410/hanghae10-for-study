package io.hhplus.ecommerce.coupon.domain.entity

import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import java.time.LocalDateTime

/**
 * 쿠폰 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 쿠폰 정보 관리 및 발급 처리
 * - 할인 금액 계산 및 검증
 * - 쿠폰 유효성 검증
 *
 * 비즈니스 규칙:
 * - 쿠폰은 발급량이 남아있고 유효기간 내에만 발급 가능
 * - 쿠폰 사용 시 최소 주문 금액 이상이어야 함
 * - 할인 타입에 따라 고정 금액 또는 퍼센트 할인 적용
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/CouponJpaEntity에서 처리됩니다.
 */
data class Coupon(
    val id: Long = 0,
    val name: String,
    val code: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val minimumOrderAmount: Long = 0,
    val totalQuantity: Int,
    var issuedQuantity: Int = 0,
    var version: Int = 0,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0
) {
    fun getRemainingQuantity(): Int = totalQuantity - issuedQuantity

    fun isAvailableForIssue(): Boolean =
        getRemainingQuantity() > 0 && isWithinValidPeriod()

    fun isValidForUse(orderAmount: Long): Boolean =
        isWithinValidPeriod() && orderAmount >= minimumOrderAmount

    fun isWithinValidPeriod(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(validFrom) && now.isBefore(validTo)
    }

    /**
     * 쿠폰을 발급하고 발급 수량을 증가
     *
     * @param issuedBy 발급자 ID
     * @throws CouponException.CouponSoldOut 쿠폰이 품절된 경우
     */
    fun issue(issuedBy: Long) {
        if (!isAvailableForIssue()) {
            throw CouponException.CouponSoldOut(name, getRemainingQuantity())
        }

        this.issuedQuantity += 1
        this.updatedBy = issuedBy
        this.updatedAt = LocalDateTime.now()
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

            val now = LocalDateTime.now()
            return Coupon(
                name = name,
                code = code,
                discountType = discountType,
                discountValue = discountValue,
                minimumOrderAmount = minimumOrderAmount,
                totalQuantity = totalQuantity,
                validFrom = validFrom,
                validTo = validTo,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

