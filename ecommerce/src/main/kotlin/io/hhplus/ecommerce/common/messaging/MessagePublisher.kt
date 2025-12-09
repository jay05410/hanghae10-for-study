package io.hhplus.ecommerce.common.messaging

/**
 * 메시지 발행 추상화 인터페이스
 *
 * Kafka 전환 대비:
 * - 현재: InMemoryMessagePublisher (Spring ApplicationEvent 기반)
 * - 전환 시: KafkaMessagePublisher (KafkaTemplate 기반)
 *
 * Profile 기반 분기:
 * - @Profile("!kafka") → InMemoryMessagePublisher
 * - @Profile("kafka") → KafkaMessagePublisher
 */
interface MessagePublisher {

    /**
     * 단건 메시지 발행
     *
     * @param topic 토픽 이름
     * @param key 메시지 키 (파티셔닝에 사용)
     * @param payload 메시지 본문
     */
    fun publish(topic: String, key: String, payload: Any)

    /**
     * 배치 메시지 발행
     *
     * @param topic 토픽 이름
     * @param messages 메시지 목록
     */
    fun publishBatch(topic: String, messages: List<Message>)
}

/**
 * 메시지 데이터 클래스
 *
 * @property key 메시지 키 (파티셔닝에 사용, 주로 aggregateId)
 * @property payload 메시지 본문
 * @property headers 메시지 헤더 (eventType, eventId, traceId 등)
 */
data class Message(
    val key: String,
    val payload: Any,
    val headers: Map<String, String> = emptyMap()
)
