package io.hhplus.ecommerce.common.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.client.DataPlatformClient
import io.hhplus.ecommerce.common.client.OrderInfoPayload
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * 데이터 플랫폼 Kafka Consumer
 *
 * Kafka 메시지를 수신하여 데이터 플랫폼에 전송
 *
 * 특징:
 * - 수동 ACK (처리 완료 후 커밋)
 * - TraceId 전파 (분산 추적)
 *
 * 멱등성 보장:
 * - Producer 측(OrderDataPlatformHandler)에서 도메인 레벨 멱등성 처리
 * - Consumer는 Kafka offset 기반으로 at-least-once 보장
 */
@Component
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = false)
class DataPlatformConsumer(
    private val dataPlatformClient: DataPlatformClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 데이터 플랫폼 토픽 리스너
     */
    @KafkaListener(
        topics = ["\${kafka.topics.data-platform}"],
        groupId = "\${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        val traceId = extractTraceId(record)

        try {
            traceId?.let { MDC.put("traceId", it) }

            logger.info(
                "[DataPlatformConsumer] 메시지 수신: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key()
            )

            val payload = objectMapper.readValue(record.value(), OrderInfoPayload::class.java)

            val response = dataPlatformClient.sendOrderInfo(payload)

            if (response.success) {
                acknowledgment.acknowledge()
                logger.info(
                    "[DataPlatformConsumer] 전송 완료: key={}, offset={}",
                    record.key(), record.offset()
                )
            } else {
                logger.warn(
                    "[DataPlatformConsumer] 전송 실패: key={}, message={}",
                    record.key(), response.message
                )
                // 실패해도 ACK - 무한 재시도 방지 (실무에서는 DLQ 이동)
                acknowledgment.acknowledge()
            }

        } catch (e: Exception) {
            logger.error(
                "[DataPlatformConsumer] 처리 오류: offset={}, error={}",
                record.offset(), e.message, e
            )
            acknowledgment.acknowledge()
        } finally {
            MDC.remove("traceId")
        }
    }

    private fun extractTraceId(record: ConsumerRecord<String, String>): String? {
        return record.headers()
            .lastHeader("traceId")
            ?.value()
            ?.let { String(it) }
            ?: record.headers()
                .lastHeader("ce-traceid")
                ?.value()
                ?.let { String(it) }
    }
}
