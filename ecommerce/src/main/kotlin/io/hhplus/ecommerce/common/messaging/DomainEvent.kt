package io.hhplus.ecommerce.common.messaging

/**
 * 도메인 이벤트 래퍼
 *
 * InMemoryMessagePublisher에서 Spring ApplicationEvent로 발행할 때 사용
 * ApplicationEventPublisher.publishEvent(DomainEvent)
 *
 * @property topic 토픽 이름 (Kafka 전환 시 실제 토픽으로 매핑)
 * @property key 메시지 키 (aggregateId)
 * @property payload 메시지 본문
 * @property headers 메시지 헤더
 */
data class DomainEvent(
    val topic: String,
    val key: String,
    val payload: Any,
    val headers: Map<String, String> = emptyMap()
) {
    /**
     * 이벤트 타입 헤더에서 추출
     */
    val eventType: String
        get() = headers["eventType"] ?: "Unknown"

    /**
     * 이벤트 ID 헤더에서 추출
     */
    val eventId: String?
        get() = headers["eventId"]

    /**
     * Trace ID 헤더에서 추출
     */
    val traceId: String?
        get() = headers["traceId"]
}
