package io.hhplus.ecommerce.common.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Kafka 기반 메시지 발행자
 *
 * kafka.enabled=true 일 때 활성화 (기본 구현체)
 *
 * 특징:
 * - CloudEvents 1.0 표준 헤더 지원
 * - TraceId 자동 전파
 * - 비동기 발행 + 콜백 로깅
 */
@Component
@Primary
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = false)
class KafkaMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : MessagePublisher {

    private val logger = KotlinLogging.logger {}

    override fun publish(topic: String, key: String, payload: Any) {
        val headers = buildHeaders()
        val record = createProducerRecord(topic, key, payload, headers)

        val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(record)

        future.whenComplete { result, ex ->
            if (ex != null) {
                logger.error(
                    "[KafkaPublisher] 메시지 발행 실패: topic={}, key={}, error={}",
                    topic, key, ex.message, ex
                )
            } else {
                logger.debug(
                    "[KafkaPublisher] 메시지 발행 성공: topic={}, key={}, partition={}, offset={}",
                    topic, key,
                    result.recordMetadata.partition(),
                    result.recordMetadata.offset()
                )
            }
        }
    }

    override fun publishBatch(topic: String, messages: List<Message>) {
        messages.forEach { msg ->
            val allHeaders = buildHeaders() + msg.headers
            val record = createProducerRecord(topic, msg.key, msg.payload, allHeaders)
            kafkaTemplate.send(record)
        }

        logger.debug(
            "[KafkaPublisher] 배치 메시지 발행: topic={}, count={}",
            topic, messages.size
        )
    }

    /**
     * ProducerRecord 생성
     *
     * CloudEvents 헤더 포함
     */
    private fun createProducerRecord(
        topic: String,
        key: String,
        payload: Any,
        headers: Map<String, String>
    ): ProducerRecord<String, Any> {
        val record = ProducerRecord<String, Any>(topic, key, payload)

        // CloudEvents 헤더 추가
        headers.forEach { (k, v) ->
            record.headers().add(RecordHeader(k, v.toByteArray()))
        }

        return record
    }

    /**
     * 기본 헤더 구성
     *
     * - traceId: MDC에서 추출 (TraceIdFilter에서 설정)
     * - ce-specversion: CloudEvents 버전
     * - ce-source: 이벤트 소스
     */
    private fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        // TraceId 전파
        MDC.get("traceId")?.let {
            headers["traceId"] = it
            headers["ce-traceid"] = it
        }

        // CloudEvents 메타데이터
        headers["ce-specversion"] = "1.0"
        headers["ce-source"] = "/ecommerce-service"

        return headers
    }
}
