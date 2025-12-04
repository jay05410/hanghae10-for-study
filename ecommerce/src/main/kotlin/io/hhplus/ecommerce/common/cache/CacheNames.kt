package io.hhplus.ecommerce.common.cache

/**
 * Spring Cache 추상화에서 사용하는 캐시 이름
 *
 * 네이밍 규칙:
 * - 형식: "ecom:cache:{domain}:{resource}"
 * - 예시: "ecom:cache:prod:dtl", "ecom:cache:cpn:info"
 *
 * 축약어 매핑:
 * - ecom = ecommerce
 * - prod = product, cpn = coupon, ord = order, usr = user
 * - dtl = detail, info = info, list = list, pop = popular, cat = category
 *
 * 주의:
 * - @Cacheable 등에서 사용되는 Spring Cache 전용
 * - Redis 직접 접근 키는 RedisKeyNames 사용
 *
 * @see RedisKeyNames Redis 직접 접근 키
 * @see docs/REDIS_KEY_STRATEGY.md 상세 문서
 */
object CacheNames {

    /** 서비스 프리픽스 */
    private const val PREFIX = "ecom:cache"

    /**
     * 쿠폰 도메인 캐시 (cpn = coupon)
     */
    const val COUPON_INFO = "$PREFIX:cpn:info"
    const val COUPON_ACTIVE_LIST = "$PREFIX:cpn:active"

    /**
     * 상품 도메인 캐시 (prod = product)
     */
    const val PRODUCT_INFO = "$PREFIX:prod:info"
    const val PRODUCT_STOCK = "$PREFIX:prod:stock"
    const val PRODUCT_DETAIL = "$PREFIX:prod:dtl"
    const val PRODUCT_LIST = "$PREFIX:prod:list"
    const val PRODUCT_CATEGORY_LIST = "$PREFIX:prod:cat"
    const val PRODUCT_POPULAR = "$PREFIX:prod:pop"
    const val PRODUCT_RANKING = "$PREFIX:prod:rank"

    /**
     * 사용자 도메인 캐시 (usr = user)
     */
    const val USER_PROFILE = "$PREFIX:usr:profile"
    const val USER_COUPON_LIST = "$PREFIX:usr:cpn"

    /**
     * 주문 도메인 캐시 (ord = order)
     */
    const val ORDER_INFO = "$PREFIX:ord:info"

    /**
     * 모든 캐시 이름 목록 (모니터링용)
     *
     * Redis CLI 사용 예:
     * redis-cli --scan --pattern "ecom:cache:*"
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
        PRODUCT_RANKING,
        USER_PROFILE,
        USER_COUPON_LIST,
        ORDER_INFO
    )
}
