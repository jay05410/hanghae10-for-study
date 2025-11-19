package io.hhplus.ecommerce.coupon.dto

import io.swagger.v3.oas.annotations.media.Schema

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
@Schema(description = "쿠폰 정보")
data class CouponResponse(
    @Schema(description = "쿠폰 ID", example = "1")
    val id: Long,

    @Schema(description = "쿠폰 이름", example = "신규회원 10% 할인")
    val name: String,

    @Schema(description = "쿠폰 코드", example = "WELCOME2025")
    val code: String,

    @Schema(description = "할인 타입", example = "PERCENTAGE", allowableValues = ["PERCENTAGE", "FIXED_AMOUNT"])
    val discountType: DiscountType,

    @Schema(description = "할인 값 (비율: %, 고정: 원)", example = "10")
    val discountValue: Long,

    @Schema(description = "최소 주문 금액", example = "30000")
    val minimumOrderAmount: Long,

    @Schema(description = "총 발급 가능 수량", example = "1000")
    val totalQuantity: Int,

    @Schema(description = "이미 발급된 수량", example = "450")
    val issuedQuantity: Int,

    @Schema(description = "남은 수량", example = "550")
    val remainingQuantity: Int,

    @Schema(description = "유효 시작 일시", example = "2025-01-01T00:00:00")
    val validFrom: LocalDateTime,

    @Schema(description = "유효 종료 일시", example = "2025-12-31T23:59:59")
    val validTo: LocalDateTime
)

fun Coupon.toResponse(): CouponResponse = CouponResponse(
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
    validTo = this.validTo
)

/**
 * 사용자 쿠폰 정보 응답 DTO - 프레젠테이션 계층
 */
@Schema(description = "사용자 쿠폰 정보")
data class UserCouponResponse(
    @Schema(description = "사용자 쿠폰 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "쿠폰 ID", example = "1")
    val couponId: Long,

    @Schema(description = "발급 일시", example = "2025-01-13T10:00:00")
    val issuedAt: LocalDateTime,

    @Schema(description = "사용 일시 (선택)", example = "2025-01-14T15:30:00")
    val usedAt: LocalDateTime?,

    @Schema(description = "사용한 주문 ID (선택)", example = "100")
    val usedOrderId: Long?,

    @Schema(description = "쿠폰 상태", example = "ISSUED", allowableValues = ["ISSUED", "USED", "EXPIRED"])
    val status: io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
)

fun io.hhplus.ecommerce.coupon.domain.entity.UserCoupon.toResponse(): UserCouponResponse = UserCouponResponse(
    id = this.id,
    userId = this.userId,
    couponId = this.couponId,
    issuedAt = this.issuedAt,
    usedAt = this.usedAt,
    usedOrderId = this.usedOrderId,
    status = this.status
)