package io.hhplus.ecommerce.pricing.domain.model

import io.hhplus.ecommerce.coupon.domain.constant.DiscountScope
import io.hhplus.ecommerce.order.domain.model.OrderItemData

/**
 * Pricing 도메인 모델 정의
 *
 * 역할:
 * - 가격 계산 요청/결과 데이터 구조 정의
 * - Order, Product, Coupon 도메인 간의 데이터 전달
 * - 할인 정보를 포함한 가격 계산 결과 캡슐화
 */

/**
 * 가격 계산 요청 아이템
 *
 * 주문 요청에서 전달받은 최소한의 정보
 */
data class PricingItemRequest(
    val productId: Long,
    val quantity: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null
)

/**
 * 상품 정보가 enriched된 아이템
 *
 * Product/Category 도메인에서 조회한 정보가 포함됨
 */
data class PricingItem(
    val productId: Long,
    val productName: String,
    val categoryId: Long,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Long,
    val giftWrap: Boolean,
    val giftMessage: String?,
    val giftWrapPrice: Long,
    val requiresReservation: Boolean
) {
    /**
     * 아이템 총 가격 (할인 전)
     * = (단가 × 수량) + 선물포장비
     */
    val itemTotalPrice: Long
        get() = (unitPrice * quantity) + if (giftWrap) giftWrapPrice else 0L
}

/**
 * 가격 계산 결과
 *
 * PricingDomainService의 최종 출력물
 */
data class PricingResult(
    val items: List<PricedItem>,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
    val appliedCouponInfo: AppliedCouponInfo? = null
) {
    /**
     * 할인 적용 여부
     */
    fun hasDiscount(): Boolean = discountAmount > 0

    /**
     * 할인율 (%)
     */
    fun getDiscountRate(): Double =
        if (totalAmount > 0) (discountAmount.toDouble() / totalAmount.toDouble()) * 100 else 0.0
}

/**
 * 가격이 계산된 개별 아이템
 *
 * OrderItemData 변환에 사용됨
 */
data class PricedItem(
    val productId: Long,
    val productName: String,
    val categoryId: Long,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Long,
    val giftWrap: Boolean,
    val giftMessage: String?,
    val giftWrapPrice: Long,
    val totalPrice: Long,
    val itemDiscountAmount: Long = 0L,
    val requiresReservation: Boolean
) {
    /**
     * 할인 적용 후 아이템 최종 가격
     */
    val finalPrice: Long
        get() = totalPrice - itemDiscountAmount
}

/**
 * 적용된 쿠폰 정보
 *
 * 주문에 적용된 쿠폰의 요약 정보
 */
data class AppliedCouponInfo(
    val userCouponId: Long,
    val couponId: Long,
    val couponName: String,
    val discountScope: DiscountScope,
    val calculatedDiscount: Long
)

/**
 * 쿠폰 검증 결과
 *
 * 쿠폰 적용 가능 여부 및 예상 할인 금액
 */
data class CouponValidationResult(
    val isValid: Boolean,
    val expectedDiscount: Long = 0L,
    val invalidReason: String? = null
) {
    companion object {
        fun valid(expectedDiscount: Long) = CouponValidationResult(
            isValid = true,
            expectedDiscount = expectedDiscount
        )

        fun invalid(reason: String) = CouponValidationResult(
            isValid = false,
            invalidReason = reason
        )
    }
}

// =============================================================================
// 확장함수: Pricing → Order 변환
// =============================================================================

/**
 * PricedItem → OrderItemData 변환
 *
 * Pricing 모듈에서 Order 모듈로 데이터를 전달할 때 사용.
 * 의존성 방향: Pricing → Order (단방향)
 */
fun PricedItem.toOrderItemData(): OrderItemData = OrderItemData(
    productId = productId,
    productName = productName,
    categoryId = categoryId,
    categoryName = categoryName,
    quantity = quantity,
    unitPrice = unitPrice.toInt(),
    giftWrap = giftWrap,
    giftMessage = giftMessage,
    giftWrapPrice = giftWrapPrice.toInt(),
    totalPrice = totalPrice.toInt(),
    requiresReservation = requiresReservation
)

/**
 * List<PricedItem> → List<OrderItemData> 변환
 */
fun List<PricedItem>.toOrderItemDataList(): List<OrderItemData> = map { it.toOrderItemData() }
