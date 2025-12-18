package io.hhplus.ecommerce.common.consumer

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.common.client.OrderInfoPayload
import io.hhplus.ecommerce.common.idempotency.IdempotencyService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
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
 * - Retry + Circuit Breaker (Resilience4j)
 *
 * 멱등성 보장 (2단계):
 * - Producer 측(OrderDataPlatformHandler): 도메인 레벨 중복 발행 방지
 * - Consumer 측(이 클래스): 중복 전송 방지 (2차 방어선)
 *
 * 에러 처리:
 * - 재시도 가능한 실패: Redis 키 삭제 → NACK → Kafka 재전달
 * - Circuit Breaker Open: 빠른 실패 → DLQ 이동
 * - 최종 실패: DLQ 이동
 */
@Component
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = false)
class DataPlatformConsumer(
    private val dataPlatformSender: DataPlatformSender,
    private val idempotencyService: IdempotencyService,
    private val redisTemplate: RedisTemplate<String, Any>
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
            val idempotencyKey = RedisKeyNames.Order.dataPlatformConsumedKey(payload.orderId, payload.status)

            // 멱등성 체크 (원자적 SETNX)
            if (!idempotencyService.tryAcquire(idempotencyKey, IDEMPOTENCY_TTL)) {
                logger.debug(
                    "[DataPlatformConsumer] 이미 처리된 메시지, 스킵: orderId={}, status={}",
                    payload.orderId, payload.status
                )
                acknowledgment.acknowledge()
                return
            }

            try {
                // Retry + Circuit Breaker가 적용된 전송
                dataPlatformSender.send(payload)

                logger.info(
                    "[DataPlatformConsumer] 전송 완료: orderId={}, offset={}",
                    payload.orderId, record.offset()
                )
                acknowledgment.acknowledge()

            } catch (e: DataPlatformException) {
                handleSendFailure(e, payload, idempotencyKey, acknowledgment, record)
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
     * - Circuit Breaker Open: 키 삭제 + NACK (Kafka 재전달 대기)
     * - 일반 실패 (Retry 소진): DLQ 이동 (실무에서는 별도 DLQ 토픽 발행)
     */
    private fun handleSendFailure(
        e: DataPlatformException,
        payload: OrderInfoPayload,
        idempotencyKey: String,
        acknowledgment: Acknowledgment,
        record: ConsumerRecord<String, String>
    ) {
        val cause = e.cause

        if (cause is CallNotPermittedException) {
            // Circuit Breaker Open 상태 → 키 삭제하고 재시도 가능하게
            logger.warn(
                "[DataPlatformConsumer] Circuit Breaker OPEN - 키 삭제 후 대기: orderId={}",
                payload.orderId
            )
            redisTemplate.delete(idempotencyKey)
            // NACK → Kafka가 재전달 (waitDurationInOpenState 후 재시도됨)
            // Spring Kafka에서는 예외를 던지면 NACK 처리
            throw e
        } else {
            // Retry 소진 후 최종 실패 → DLQ 이동 (여기서는 로그만)
            logger.error(
                "[DataPlatformConsumer] 최종 실패 (DLQ 이동): orderId={}, offset={}, error={}",
                payload.orderId, record.offset(), e.message
            )
            // 키 유지 (중복 처리 방지) + ACK (DLQ 처리로 간주)
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
