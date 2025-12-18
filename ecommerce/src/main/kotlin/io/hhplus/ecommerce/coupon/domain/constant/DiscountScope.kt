package io.hhplus.ecommerce.coupon.domain.constant

/**
 * 쿠폰 할인 적용 범위
 *
 * 쿠폰이 어느 범위에 할인을 적용할지 결정:
 * - TOTAL: 전체 주문 금액에 할인 적용
 * - CATEGORY: 특정 카테고리 상품에만 할인 적용
 * - PRODUCT: 특정 상품에만 할인 적용
 */
enum class DiscountScope {
    /**
     * 전체 금액 할인
     *
     * 주문의 총 금액에 대해 할인을 적용.
     * 기본값으로 사용됨.
     */
    TOTAL,

    /**
     * 카테고리별 할인
     *
     * targetCategoryIds에 지정된 카테고리에 속한
     * 상품 금액 합계에 대해서만 할인 적용.
     */
    CATEGORY,

    /**
     * 상품별 할인
     *
     * targetProductIds에 지정된 상품의
     * 금액 합계에 대해서만 할인 적용.
     */
    PRODUCT
}
