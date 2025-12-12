package io.hhplus.ecommerce.common.consumer

import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.common.client.DataPlatformClient
import io.hhplus.ecommerce.common.client.OrderInfoPayload
import io.hhplus.ecommerce.common.idempotency.IdempotencyService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 데이터 플랫폼 Kafka Consumer
 *
 * Kafka 메시지를 수신하여 데이터 플랫폼에 전송
 *
 * 특징:
 * - 수동 ACK (처리 완료 후 커밋)
 * - TraceId 전파 (분산 추적)
 *
 * 멱등성 보장 (2단계):
 * - Producer 측(OrderDataPlatformHandler): 도메인 레벨 중복 발행 방지
 * - Consumer 측(이 클래스): 중복 전송 방지 (2차 방어선)
 *   → Kafka at-least-once 특성상 ACK 전 실패 시 재전달될 수 있음
 */
@Component
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = false)
class DataPlatformConsumer(
    private val dataPlatformClient: DataPlatformClient,
    private val idempotencyService: IdempotencyService
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val IDEMPOTENCY_TTL = Duration.ofDays(7)
    }

    /**
     * 데이터 플랫폼 토픽 리스너
     */
    @KafkaListener(
        topics = ["\${kafka.topics.data-platform}"],
        groupId = "\${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )

    /**
    * 데이터 플랫폼 메시지 소비
    * @param record Kafka 레코드
    * @param acknowledgment 수동 ACK 핸들러
    */
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

            val payload = json.decodeFromString<OrderInfoPayload>(record.value())

            // Consumer 레벨 멱등성 체크 (2차 방어선) - 원자적 SETNX
            val idempotencyKey = RedisKeyNames.Order.dataPlatformConsumedKey(payload.orderId, payload.status)
            if (!idempotencyService.tryAcquire(idempotencyKey, IDEMPOTENCY_TTL)) {
                logger.debug(
                    "[DataPlatformConsumer] 이미 처리된 메시지, 스킵: orderId={}, status={}",
                    payload.orderId, payload.status
                )
                acknowledgment.acknowledge()
                return
            }

            val response = dataPlatformClient.sendOrderInfo(payload)

            if (response.success) {
                acknowledgment.acknowledge()
                logger.info(
                    "[DataPlatformConsumer] 전송 완료: orderId={}, key={}, offset={}",
                    payload.orderId, record.key(), record.offset()
                )
            } else {
                logger.warn(
                    "[DataPlatformConsumer] 전송 실패: orderId={}, key={}, message={}",
                    payload.orderId, record.key(), response.message
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
