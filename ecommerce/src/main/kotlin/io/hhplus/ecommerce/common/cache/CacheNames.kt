package io.hhplus.ecommerce.common.cache

/**
 * 애플리케이션 전체에서 사용되는 캐시 네이밍 컨벤션
 *
 * 네이밍 규칙:
 * - 형식: "{domain}:{entity}:{optional_subtype}"
 * - 예시: "ecommerce:coupon", "ecommerce:product:detail"
 *
 * 이유:
 * - 캐시 키 충돌 방지
 * - 일관된 네이밍으로 관리 용이
 * - 도메인별 그룹화로 모니터링 편리
 */
object CacheNames {

    /**
     * 쿠폰 도메인 캐시
     */
    const val COUPON_INFO = "ecommerce:coupon:info"
    const val COUPON_ACTIVE_LIST = "ecommerce:coupon:active_list"

    /**
     * 상품 도메인 캐시
     */
    const val PRODUCT_INFO = "ecommerce:product:info"
    const val PRODUCT_STOCK = "ecommerce:product:stock"
    const val PRODUCT_DETAIL = "ecommerce:product:detail"
    const val PRODUCT_LIST = "ecommerce:product:list"
    const val PRODUCT_CATEGORY_LIST = "ecommerce:product:category_list"
    const val PRODUCT_POPULAR = "ecommerce:product:popular"

    /**
     * 사용자 도메인 캐시 (향후 확장용)
     */
    const val USER_PROFILE = "ecommerce:user:profile"
    const val USER_COUPON_LIST = "ecommerce:user:coupon_list"

    /**
     * 주문 도메인 캐시 (향후 확장용)
     */
    const val ORDER_INFO = "ecommerce:order:info"

    /**
     * 모든 캐시 이름 목록 (모니터링용)
     */
    val ALL_CACHE_NAMES = listOf(
        COUPON_INFO,
        COUPON_ACTIVE_LIST,
        PRODUCT_INFO,
        PRODUCT_STOCK,
        PRODUCT_DETAIL,
        PRODUCT_LIST,
        PRODUCT_CATEGORY_LIST,
        PRODUCT_POPULAR,
        USER_PROFILE,
        USER_COUPON_LIST,
        ORDER_INFO
    )
}