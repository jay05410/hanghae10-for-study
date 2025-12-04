package io.hhplus.ecommerce.coupon.domain.vo

import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import java.io.Serializable
import java.time.LocalDateTime

/**
 * 쿠폰 캐시용 VO (Value Object)
 *
 * 역할:
 * - 로컬 캐시(Caffeine)에 저장되는 쿠폰 정적 정보
 * - 동적 데이터(totalQuantity, issuedQuantity)는 제외
 *
 * 설계 원칙:
 * - 정적 정보만 캐싱: 할인 정보, 유효기간 등 변경 빈도 낮은 데이터
 * - 동적 정보는 Redis: 발급 수량은 Redis maxQuantity로 관리
 * - 캐시 무효화 최소화: totalQuantity 변경해도 로컬 캐시 영향 없음
 *
 * @see Coupon 원본 엔티티
 */
data class CouponCacheInfo(
    val id: Long,
    val name: String,
    val code: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val minimumOrderAmount: Long,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        /**
         * Coupon 엔티티에서 캐시용 VO 생성
         */
        fun from(coupon: Coupon): CouponCacheInfo = CouponCacheInfo(
            id = coupon.id,
            name = coupon.name,
            code = coupon.code,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minimumOrderAmount = coupon.minimumOrderAmount,
            validFrom = coupon.validFrom,
            validTo = coupon.validTo
        )
    }

    /**
     * 현재 유효한 쿠폰인지 확인
     */
    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(validFrom) && now.isBefore(validTo)
    }

    /**
     * 주문 금액이 최소 주문 금액 이상인지 확인
     */
    fun isValidForUse(orderAmount: Long): Boolean {
        return orderAmount >= minimumOrderAmount
    }

    /**
     * 할인 금액 계산
     */
    fun calculateDiscountAmount(orderAmount: Long): Long {
        return when (discountType) {
            DiscountType.FIXED -> discountValue
            DiscountType.PERCENTAGE -> (orderAmount * discountValue / 100)
        }
    }
}
