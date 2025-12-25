package io.hhplus.ecommerce.common.consumer

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.hhplus.ecommerce.common.client.OrderInfoPayload
import io.hhplus.ecommerce.common.messaging.Topics
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
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
 * - Retry + Circuit Breaker (Resilience4j)
 *
 * 멱등성 보장:
 * - Kafka idempotent producer: Producer → Broker 중복 방지
 * - X-Idempotency-Key 헤더: 외부 API에서 중복 처리
 *
 * 에러 처리:
 * - Circuit Breaker Open: 예외 발생 → Kafka 재전달
 * - 최종 실패: ACK 후 DLQ 처리로 간주
 */
@Component
class DataPlatformConsumer(
    private val dataPlatformSender: DataPlatformSender
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 데이터 플랫폼 토픽 리스너
     */
    @KafkaListener(
        topics = [Topics.DATA_PLATFORM],
        groupId = "#{kafkaProperties.consumer.dataPlatformGroupId}",
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

            val payload = json.decodeFromString<OrderInfoPayload>(record.value())

            try {
                // Retry + Circuit Breaker가 적용된 전송
                // X-Idempotency-Key 헤더로 외부 API에서 중복 처리
                dataPlatformSender.send(payload)

                logger.info(
                    "[DataPlatformConsumer] 전송 완료: orderId={}, offset={}",
                    payload.orderId, record.offset()
                )
                acknowledgment.acknowledge()

            } catch (e: DataPlatformException) {
                handleSendFailure(e, payload, acknowledgment, record)
            }

        } catch (e: Exception) {
            logger.error(
                "[DataPlatformConsumer] 처리 오류: offset={}, error={}",
                record.offset(), e.message, e
            )
            // 파싱 오류 등은 재시도해도 의미 없음 → ACK 후 스킵
            acknowledgment.acknowledge()
        } finally {
            MDC.remove("traceId")
        }
    }

    /**
     * 전송 실패 처리
     *
     * - Circuit Breaker Open: 예외 던져서 Kafka 재전달 대기
     * - 일반 실패 (Retry 소진): ACK 후 DLQ 처리로 간주
     */
    private fun handleSendFailure(
        e: DataPlatformException,
        payload: OrderInfoPayload,
        acknowledgment: Acknowledgment,
        record: ConsumerRecord<String, String>
    ) {
        val cause = e.cause

        if (cause is CallNotPermittedException) {
            // Circuit Breaker Open 상태 → 예외를 던져서 Kafka가 재전달하도록
            logger.warn(
                "[DataPlatformConsumer] Circuit Breaker OPEN - 재시도 대기: orderId={}",
                payload.orderId
            )
            throw e
        } else {
            // Retry 소진 후 최종 실패 → DLQ 이동 (실무에서는 별도 DLQ 토픽 발행)
            logger.error(
                "[DataPlatformConsumer] 최종 실패 (DLQ 이동): orderId={}, offset={}, error={}",
                payload.orderId, record.offset(), e.message
            )
            acknowledgment.acknowledge()
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
