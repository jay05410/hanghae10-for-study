package io.hhplus.ecommerce.common.messaging

import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import org.slf4j.MDC
import java.time.Instant

/**
 * CloudEvents 표준 기반 이벤트 래퍼
 *
 * CloudEvents Specification v1.0 준수
 * https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md
 *
 * Kafka 전환 시 이벤트 스키마 표준으로 사용
 * - specversion: CloudEvents 버전
 * - id: 이벤트 고유 식별자 (Snowflake ID)
 * - source: 이벤트 발생 출처
 * - type: 이벤트 타입 (네임스페이스 포함)
 * - subject: 대상 리소스 식별자 (aggregateId)
 * - time: 이벤트 발생 시각
 * - datacontenttype: 데이터 형식
 * - data: 이벤트 페이로드
 *
 * 확장 속성:
 * - traceid: 분산 추적 ID (Snowflake 기반)
 * - correlationid: 연관 이벤트 ID
 *
 * @param T 페이로드 데이터 타입
 */
data class CloudEvent<T>(
    val specversion: String = "1.0",
    val id: String,
    val source: String,
    val type: String,
    val subject: String?,
    val time: Instant = Instant.now(),
    val datacontenttype: String = "application/json",
    val data: T,
    val traceid: String,
    val correlationid: String? = null
) {
    companion object {
        /**
         * CloudEvent 생성 팩토리 메서드
         *
         * @param snowflakeGenerator Snowflake ID 생성기
         * @param source 이벤트 발생 출처 (예: "/order-service")
         * @param type 이벤트 타입 (예: "io.hhplus.ecommerce.order.completed")
         * @param subject 대상 리소스 ID (aggregateId)
         * @param data 이벤트 페이로드
         * @param correlationId 연관 이벤트 ID (선택)
         */
        fun <T> create(
            snowflakeGenerator: SnowflakeGenerator,
            source: String,
            type: String,
            subject: String?,
            data: T,
            correlationId: String? = null
        ): CloudEvent<T> {
            // Snowflake ID를 16진수 대문자로 변환
            val eventId = snowflakeGenerator.nextId().toString(16).uppercase()

            // MDC에서 traceId 추출, 없으면 새로 생성
            val traceId = MDC.get("traceId")
                ?: snowflakeGenerator.nextId().toString(16).uppercase()

            return CloudEvent(
                id = eventId,
                source = source,
                type = type,
                subject = subject,
                data = data,
                traceid = traceId,
                correlationid = correlationId
            )
        }
    }

    /**
     * Kafka 헤더로 변환
     */
    fun toHeaders(): Map<String, String> {
        return buildMap {
            put("ce-specversion", specversion)
            put("ce-id", id)
            put("ce-source", source)
            put("ce-type", type)
            subject?.let { put("ce-subject", it) }
            put("ce-time", time.toString())
            put("ce-datacontenttype", datacontenttype)
            put("ce-traceid", traceid)
            correlationid?.let { put("ce-correlationid", it) }
        }
    }
}

/**
 * CloudEvents 이벤트 타입 정의
 *
 * 형식: io.hhplus.ecommerce.{domain}.{action}
 * 기존 EventRegistry.EventTypes를 CloudEvents 형식으로 확장
 */
object CloudEventTypes {
    private const val PREFIX = "io.hhplus.ecommerce"

    // Order 도메인
    const val ORDER_CREATED = "$PREFIX.order.created"
    const val ORDER_CANCELLED = "$PREFIX.order.cancelled"
    const val ORDER_CONFIRMED = "$PREFIX.order.confirmed"

    // Payment 도메인
    const val PAYMENT_COMPLETED = "$PREFIX.payment.completed"
    const val PAYMENT_FAILED = "$PREFIX.payment.failed"

    // Inventory 도메인
    const val STOCK_DEDUCTED = "$PREFIX.inventory.stock-deducted"
    const val STOCK_RESTORED = "$PREFIX.inventory.stock-restored"

    // Coupon 도메인
    const val COUPON_USED = "$PREFIX.coupon.used"
    const val COUPON_RESTORED = "$PREFIX.coupon.restored"
    const val COUPON_ISSUED = "$PREFIX.coupon.issued"

    // Point 도메인
    const val POINT_USED = "$PREFIX.point.used"
    const val POINT_REFUNDED = "$PREFIX.point.refunded"
}

/**
 * Kafka 토픽명 상수 (단일 소스)
 *
 * 사용처:
 * - KafkaTopicConfig: 토픽 자동 생성
 * - KafkaMessagePublisher: 토픽 라우팅
 * - @KafkaListener: 토픽 지정
 * - EventHandler: 토픽 기반 필터링
 */
object Topics {
    const val ORDER = "ecommerce.order"
    const val PAYMENT = "ecommerce.payment"
    const val INVENTORY = "ecommerce.inventory"
    const val COUPON = "ecommerce.coupon"
    const val POINT = "ecommerce.point"
    const val DELIVERY = "ecommerce.delivery"
    const val DATA_PLATFORM = "ecommerce.data-platform"
    const val DEFAULT = "ecommerce.default"

    /**
     * 이벤트 타입에서 토픽 매핑
     */
    fun fromEventType(eventType: String): String {
        return when {
            eventType.contains("order", ignoreCase = true) ||
                eventType.contains("Order") -> ORDER
            eventType.contains("payment", ignoreCase = true) ||
                eventType.contains("Payment") -> PAYMENT
            eventType.contains("inventory", ignoreCase = true) ||
                eventType.contains("Stock") -> INVENTORY
            eventType.contains("coupon", ignoreCase = true) ||
                eventType.contains("Coupon") -> COUPON
            eventType.contains("point", ignoreCase = true) ||
                eventType.contains("Point") -> POINT
            eventType.contains("delivery", ignoreCase = true) ||
                eventType.contains("Delivery") -> DELIVERY
            else -> DEFAULT
        }
    }
}
