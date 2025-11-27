package io.hhplus.ecommerce.common.lock

/**
 * 애플리케이션 전체에서 사용되는 분산락 키 컨벤션
 *
 * 네이밍 규칙:
 * - 형식: "{domain}:{operation}:{resource}:#{#{#parameter}}"
 * - 예시: "coupon:issue:user:#{#userId}", "order:create:user:#{#userId}"
 *
 * 이유:
 * - 분산락 키 충돌 방지
 * - 일관된 네이밍으로 관리 용이
 * - 도메인별 그룹화로 디버깅 편리
 * - SpEL 표현식 표준화
 */
object DistributedLockKeys {

    /**
     * 쿠폰 도메인 분산락 키
     */
    object Coupon {
        const val ISSUE = "coupon:issue:#{#request.couponId}"
        const val ENQUEUE = "coupon:enqueue:#{#coupon.id}"
        const val USE = "coupon:use:user:#{#userId}"
        const val VALIDATE = "coupon:validate:user:#{#userId}"
    }

    /**
     * 포인트 도메인 분산락 키
     */
    object Point {
        const val CHARGE = "point:charge:user:#{#userId}"
        const val USE = "point:use:user:#{#userId}"
        const val EARN = "point:earn:user:#{#userId}"
        const val DEDUCT = "point:deduct:user:#{#userId}"
        const val EXPIRE = "point:expire:user:#{#userId}"
    }

    /**
     * 주문 도메인 분산락 키
     */
    object Order {
        const val CREATE = "order:create:user:#{#userId}"
        const val CANCEL = "order:cancel:#{#orderId}"
        const val CONFIRM = "order:confirm:#{#orderId}"
        const val PROCESS = "order:process:user:#{#request.userId}"
    }

    /**
     * 결제 도메인 분산락 키
     */
    object Payment {
        const val PROCESS = "payment:process:order:#{#orderId}"
        const val DUPLICATE_PREVENTION = "payment:order:#{#orderId}"
    }

    /**
     * 재고 도메인 분산락 키
     */
    object Inventory {
        const val DEDUCT = "inventory:deduct:product:#{#productId}"
        const val RESERVE = "inventory:reserve:product:#{#productId}"
        const val CONFIRM_RESERVATION = "inventory:confirm:product:#{#productId}"
        const val CANCEL_RESERVATION = "inventory:cancel:product:#{#productId}"
        const val RESTOCK = "inventory:restock:product:#{#productId}"
    }

    /**
     * 모든 분산락 키 목록 (모니터링용)
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

        // Inventory
        Inventory.DEDUCT,
        Inventory.RESERVE,
        Inventory.CONFIRM_RESERVATION,
        Inventory.CANCEL_RESERVATION,
        Inventory.RESTOCK
    )

    /**
     * 도메인별 분산락 키 그룹핑 (모니터링/디버깅용)
     */
    val GROUPED_KEYS = mapOf(
        "coupon" to listOf(Coupon.ISSUE, Coupon.USE, Coupon.VALIDATE),
        "point" to listOf(Point.CHARGE, Point.USE, Point.EARN, Point.DEDUCT, Point.EXPIRE),
        "order" to listOf(Order.CREATE, Order.CANCEL, Order.CONFIRM, Order.PROCESS),
        "payment" to listOf(Payment.PROCESS, Payment.DUPLICATE_PREVENTION),
        "inventory" to listOf(Inventory.DEDUCT, Inventory.RESERVE, Inventory.CONFIRM_RESERVATION, Inventory.CANCEL_RESERVATION, Inventory.RESTOCK)
    )
}