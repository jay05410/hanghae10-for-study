package io.hhplus.ecommerce.common.lock

/**
 * 분산락 키 중앙 관리
 *
 * 네이밍 규칙:
 * - 형식: "ecom:lock:{domain}:{operation}:#{parameter}"
 * - 예시: "ecom:lock:cpn:issue:#{#request.couponId}"
 *
 * 축약어 매핑:
 * - ecom = ecommerce
 * - cpn = coupon, pt = point, ord = order, pay = payment, inv = inventory
 *
 * SpEL 표현식:
 * - #{#변수명} 형식으로 동적 키 생성
 * - AOP에서 파라미터 값으로 치환됨
 *
 * @see docs/REDIS_KEY_STRATEGY.md 상세 문서
 */
object DistributedLockKeys {

    /** 서비스 프리픽스 */
    private const val PREFIX = "ecom:lock"

    /**
     * 쿠폰 도메인 분산락 (cpn = coupon)
     */
    object Coupon {
        private const val DOMAIN = "$PREFIX:cpn"

        const val ISSUE = "$DOMAIN:issue:#{#request.couponId}"
        const val ENQUEUE = "$DOMAIN:enqueue:#{#coupon.id}"
        const val USE = "$DOMAIN:use:#{#userId}"
        const val VALIDATE = "$DOMAIN:validate:#{#userId}"
    }

    /**
     * 포인트 도메인 분산락 (pt = point)
     */
    object Point {
        private const val DOMAIN = "$PREFIX:pt"

        const val CHARGE = "$DOMAIN:charge:#{#userId}"
        const val USE = "$DOMAIN:use:#{#userId}"
        const val EARN = "$DOMAIN:earn:#{#userId}"
        const val DEDUCT = "$DOMAIN:deduct:#{#userId}"
        const val EXPIRE = "$DOMAIN:expire:#{#userId}"
    }

    /**
     * 주문 도메인 분산락 (ord = order)
     */
    object Order {
        private const val DOMAIN = "$PREFIX:ord"

        const val CREATE = "$DOMAIN:create:#{#userId}"
        const val CANCEL = "$DOMAIN:cancel:#{#orderId}"
        const val CONFIRM = "$DOMAIN:confirm:#{#orderId}"
        const val PROCESS = "$DOMAIN:process:#{#request.userId}"
    }

    /**
     * 결제 도메인 분산락 (pay = payment)
     */
    object Payment {
        private const val DOMAIN = "$PREFIX:pay"

        const val PROCESS = "$DOMAIN:process:#{#orderId}"
        const val DUPLICATE_PREVENTION = "$DOMAIN:dup:#{#orderId}"
        const val REFUND = "$DOMAIN:refund:#{#paymentId}"
    }

    /**
     * 재고 도메인 분산락 (inv = inventory)
     */
    object Inventory {
        private const val DOMAIN = "$PREFIX:inv"

        const val DEDUCT = "$DOMAIN:deduct:#{#productId}"
        const val RESERVE = "$DOMAIN:reserve:#{#productId}"
        const val CONFIRM_RESERVATION = "$DOMAIN:confirm:#{#productId}"
        const val CANCEL_RESERVATION = "$DOMAIN:cancel:#{#productId}"
        const val RESTOCK = "$DOMAIN:restock:#{#productId}"
    }

    /**
     * 체크아웃 도메인 분산락 (chk = checkout)
     */
    object Checkout {
        private const val DOMAIN = "$PREFIX:chk"

        const val INITIATE = "$DOMAIN:init:#{#request.userId}"
        const val CANCEL = "$DOMAIN:cancel:#{#orderId}"
    }

    /**
     * 모든 분산락 키 패턴 목록 (모니터링용)
     *
     * Redis CLI 사용 예:
     * redis-cli --scan --pattern "ecom:lock:*"
     */
    val ALL_LOCK_KEYS = listOf(
        // Coupon
        Coupon.ISSUE,
        Coupon.USE,
        Coupon.VALIDATE,

        // Point
        Point.CHARGE,
        Point.USE,
        Point.EARN,
        Point.DEDUCT,
        Point.EXPIRE,

        // Order
        Order.CREATE,
        Order.CANCEL,
        Order.CONFIRM,
        Order.PROCESS,

        // Payment
        Payment.PROCESS,
        Payment.DUPLICATE_PREVENTION,
        Payment.REFUND,

        // Inventory
        Inventory.DEDUCT,
        Inventory.RESERVE,
        Inventory.CONFIRM_RESERVATION,
        Inventory.CANCEL_RESERVATION,
        Inventory.RESTOCK,

        // Checkout
        Checkout.INITIATE,
        Checkout.CANCEL
    )

    /**
     * 도메인별 분산락 키 그룹핑 (모니터링/디버깅용)
     */
    val GROUPED_KEYS = mapOf(
        "coupon" to listOf(Coupon.ISSUE, Coupon.USE, Coupon.VALIDATE),
        "point" to listOf(Point.CHARGE, Point.USE, Point.EARN, Point.DEDUCT, Point.EXPIRE),
        "order" to listOf(Order.CREATE, Order.CANCEL, Order.CONFIRM, Order.PROCESS),
        "payment" to listOf(Payment.PROCESS, Payment.DUPLICATE_PREVENTION, Payment.REFUND),
        "inventory" to listOf(Inventory.DEDUCT, Inventory.RESERVE, Inventory.CONFIRM_RESERVATION, Inventory.CANCEL_RESERVATION, Inventory.RESTOCK),
        "checkout" to listOf(Checkout.INITIATE, Checkout.CANCEL)
    )
}
