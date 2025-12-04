package io.hhplus.ecommerce.common.cache

/**
 * Redis 키 네이밍 중앙 관리
 *
 * 네이밍 규칙:
 * - 형식: "{service}:{domain}:{purpose}:{identifier}"
 * - 예시: "ecom:stat:rt:view:130:29876543"
 *
 * 축약어 원칙:
 * - 극단적 축약(2자) 지양 → 가독성 유지
 * - 중간 수준 축약(3-5자) 사용 → 메모리 최적화
 * - 모든 축약어는 이 파일 상단에 문서화
 *
 * 축약어 매핑:
 * - ecom = ecommerce (서비스)
 * - stat = statistics, cpn = coupon, prod = product, pt = point, rank = ranking (도메인)
 * - rt = realtime, cache = cache, lock = lock, queue = queue, iss = issue (목적)
 * - view, sales, wish, pop = popular, dtl = detail, d = daily, w = weekly (리소스)
 *
 * 사용 규칙:
 * - 개발자는 반드시 이 object의 함수를 통해 키 생성
 * - 직접 문자열 작성 금지 (오타/불일치 방지)
 *
 * @see docs/REDIS_KEY_STRATEGY.md 상세 문서
 */
object RedisKeyNames {

    /** 서비스 프리픽스 */
    private const val SVC = "ecom"

    /**
     * 통계 관련 키 (Statistics)
     *
     * 용도: 실시간 조회수, 판매량, 찜 수, 인기 상품
     */
    object Stats {
        private const val DOMAIN = "$SVC:stat"

        // 실시간 통계 (rt = realtime)
        const val RT_VIEW = "$DOMAIN:rt:view"
        const val RT_SALES = "$DOMAIN:rt:sales"
        const val RT_WISH = "$DOMAIN:rt:wish"

        // 인기 상품 (pop = popular, win = window)
        const val POP_WINDOW = "$DOMAIN:pop:win"

        // 이벤트 로그
        const val EVENT_LOG = "$DOMAIN:log"

        /** 조회수 키: ecom:stat:rt:view:{productId}:{minute} */
        fun viewKey(productId: Long, minute: Long): String =
            "$RT_VIEW:$productId:$minute"

        /** 판매량 키: ecom:stat:rt:sales:{productId} */
        fun salesKey(productId: Long): String =
            "$RT_SALES:$productId"

        /** 찜 키: ecom:stat:rt:wish:{productId} */
        fun wishKey(productId: Long): String =
            "$RT_WISH:$productId"

        /** 인기 상품 윈도우 키: ecom:stat:pop:win:{windowId} */
        fun popularWindowKey(windowId: Long): String =
            "$POP_WINDOW:$windowId"

        /** 이벤트 로그 키: ecom:stat:log:{hour} */
        fun eventLogKey(hour: Long): String =
            "$EVENT_LOG:$hour"
    }

    /**
     * 상품 판매 랭킹 관련 키 (Ranking)
     *
     * 용도: 판매량 기반 상품 랭킹 (Sorted Set)
     * 명령어: ZINCRBY, ZREVRANGE, ZRANK, ZSCORE
     *
     */
    object Ranking {
        private const val DOMAIN = "$SVC:rank"

        // sales = 판매량 랭킹, d = daily, w = weekly
        const val SALES_DAILY = "$DOMAIN:sales:d"
        const val SALES_WEEKLY = "$DOMAIN:sales:w"
        const val SALES_TOTAL = "$DOMAIN:sales:total"

        /** 일별 판매 랭킹 키: ecom:rank:sales:d:{yyyyMMdd} */
        fun dailySalesKey(date: String): String =
            "$SALES_DAILY:$date"

        /** 주간 판매 랭킹 키: ecom:rank:sales:w:{yyyyWW} */
        fun weeklySalesKey(yearWeek: String): String =
            "$SALES_WEEKLY:$yearWeek"

        /** 누적 판매 랭킹 키: ecom:rank:sales:total */
        fun totalSalesKey(): String = SALES_TOTAL
    }

    /**
     * 선착순 쿠폰 발급 관련 키 (Coupon Issue)
     *
     * 용도: 선착순 쿠폰 발급 (SET + ZSET 기반)
     * - issued: 발급된 유저 목록 (SET) - SADD 원자성으로 중복 방지
     * - queue: 발급 대기열 (ZSET) - timestamp score로 선착순 보장
     *
     */
    object CouponIssue {
        private const val DOMAIN = "$SVC:cpn:iss"

        const val ISSUED = "$DOMAIN:issued"
        const val QUEUE = "$DOMAIN:queue"
        const val COUNTER = "$DOMAIN:cnt"
        const val SOLDOUT = "$DOMAIN:soldout"
        const val MAX_QTY = "$DOMAIN:max"

        /** 발급된 유저 SET 키: ecom:cpn:iss:issued:{couponId} */
        fun issuedKey(couponId: Long): String =
            "$ISSUED:$couponId"

        /** 발급 대기열 ZSET 키: ecom:cpn:iss:queue:{couponId} */
        fun queueKey(couponId: Long): String =
            "$QUEUE:$couponId"

        /** 발급 카운터 STRING 키: ecom:cpn:iss:cnt:{couponId} */
        fun counterKey(couponId: Long): String =
            "$COUNTER:$couponId"

        /** 매진 플래그 STRING 키: ecom:cpn:iss:soldout:{couponId} */
        fun soldoutKey(couponId: Long): String =
            "$SOLDOUT:$couponId"

        /** 최대 발급 수량 STRING 키: ecom:cpn:iss:max:{couponId} */
        fun maxQuantityKey(couponId: Long): String =
            "$MAX_QTY:$couponId"
    }

    /**
     * 분산락 관련 키 (Lock)
     *
     * 용도: 분산 환경에서 동시성 제어
     * 참고: DistributedLockKeys.kt에서 사용
     */
    object Lock {
        private const val DOMAIN = "$SVC:lock"

        const val ORDER = "$DOMAIN:ord"
        const val POINT = "$DOMAIN:pt"
        const val COUPON = "$DOMAIN:cpn"
        const val INVENTORY = "$DOMAIN:inv"
        const val PAYMENT = "$DOMAIN:pay"
    }

    /**
     * 캐시 관련 키 (Cache)
     *
     * 용도: Spring Cache 추상화에서 사용
     * 참고: CacheNames.kt와 연계
     */
    object Cache {
        private const val DOMAIN = "$SVC:cache"

        // prod = product, cpn = coupon
        const val PRODUCT_DETAIL = "$DOMAIN:prod:dtl"
        const val PRODUCT_LIST = "$DOMAIN:prod:list"
        const val PRODUCT_POPULAR = "$DOMAIN:prod:pop"
        const val PRODUCT_CATEGORY = "$DOMAIN:prod:cat"
        const val COUPON_INFO = "$DOMAIN:cpn:info"
        const val COUPON_ACTIVE = "$DOMAIN:cpn:active"
    }

    /**
     * 모든 키 프리픽스 목록 (모니터링용)
     *
     * Redis CLI에서 패턴 검색 시 사용:
     * redis-cli --scan --pattern "ecom:stat:*"
     */
    val ALL_PREFIXES = listOf(
        // Stats
        Stats.RT_VIEW,
        Stats.RT_SALES,
        Stats.RT_WISH,
        Stats.POP_WINDOW,
        Stats.EVENT_LOG,

        // Ranking
        Ranking.SALES_DAILY,
        Ranking.SALES_WEEKLY,
        Ranking.SALES_TOTAL,

        // Coupon Issue
        CouponIssue.ISSUED,
        CouponIssue.QUEUE,

        // Lock
        Lock.ORDER,
        Lock.POINT,
        Lock.COUPON,
        Lock.INVENTORY,
        Lock.PAYMENT,

        // Cache
        Cache.PRODUCT_DETAIL,
        Cache.PRODUCT_LIST,
        Cache.PRODUCT_POPULAR,
        Cache.PRODUCT_CATEGORY,
        Cache.COUPON_INFO,
        Cache.COUPON_ACTIVE
    )
}
