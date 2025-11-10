package io.hhplus.ecommerce.coupon.dto

import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import java.time.LocalDateTime

/**
 * 쿠폰 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - Coupon 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
data class CouponResponse(
    val id: Long,
    val name: String,
    val code: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val minimumOrderAmount: Long,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val remainingQuantity: Int,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun io.hhplus.ecommerce.coupon.domain.entity.Coupon.toResponse(): CouponResponse = CouponResponse(
            id = this.id,
            name = this.name,
            code = this.code,
            discountType = this.discountType,
            discountValue = this.discountValue,
            minimumOrderAmount = this.minimumOrderAmount,
            totalQuantity = this.totalQuantity,
            issuedQuantity = this.issuedQuantity,
            remainingQuantity = this.getRemainingQuantity(),
            validFrom = this.validFrom,
            validTo = this.validTo,
            isActive = this.isActive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

/**
 * 사용자 쿠폰 정보 응답 DTO - 프레젠테이션 계층
 */
data class UserCouponResponse(
    val id: Long,
    val userId: Long,
    val couponId: Long,
    val issuedAt: LocalDateTime,
    val usedAt: LocalDateTime?,
    val usedOrderId: Long?,
    val status: io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun io.hhplus.ecommerce.coupon.domain.entity.UserCoupon.toResponse(): UserCouponResponse = UserCouponResponse(
            id = this.id,
            userId = this.userId,
            couponId = this.couponId,
            issuedAt = this.issuedAt,
            usedAt = this.usedAt,
            usedOrderId = this.usedOrderId,
            status = this.status,
            isActive = this.isActive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}