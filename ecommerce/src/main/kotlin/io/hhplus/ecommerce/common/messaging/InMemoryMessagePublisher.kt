package io.hhplus.ecommerce.common.messaging

import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 인메모리 메시지 발행자
 *
 * Spring ApplicationEventPublisher를 사용하여 동일 JVM 내에서 이벤트 발행
 * Kafka 전환 전까지 사용하는 기본 구현체
 *
 * Profile: !kafka (kafka 프로파일이 아닐 때 활성화)
 */
@Component
@Profile("!kafka")
class InMemoryMessagePublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : MessagePublisher {

    private val logger = KotlinLogging.logger {}

    override fun publish(topic: String, key: String, payload: Any) {
        val headers = buildHeaders()

        val event = DomainEvent(
            topic = topic,
            key = key,
            payload = payload,
            headers = headers
        )

        logger.debug(
            "[InMemoryPublisher] 이벤트 발행: topic={}, key={}, eventType={}",
            topic, key, headers["eventType"]
        )

        applicationEventPublisher.publishEvent(event)
    }

    override fun publishBatch(topic: String, messages: List<Message>) {
        messages.forEach { msg ->
            val allHeaders = buildHeaders() + msg.headers

            val event = DomainEvent(
                topic = topic,
                key = msg.key,
                payload = msg.payload,
                headers = allHeaders
            )

            applicationEventPublisher.publishEvent(event)
        }

        logger.debug(
            "[InMemoryPublisher] 배치 이벤트 발행: topic={}, count={}",
            topic, messages.size
        )
    }

    /**
     * 기본 헤더 구성
     * - traceId: MDC에서 추출 (TraceIdFilter에서 설정)
     */
    private fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        MDC.get("traceId")?.let {
            headers["traceId"] = it
        }

        return headers
    }
}
