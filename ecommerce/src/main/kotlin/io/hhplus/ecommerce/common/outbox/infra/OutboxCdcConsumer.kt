package io.hhplus.ecommerce.common.outbox.infra

import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.common.outbox.dlq.DlqService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Outbox CDC Consumer
 *
 * Debezium CDC가 outbox_events 테이블의 INSERT를 감지하여
 * Kafka로 발행한 메시지를 실시간으로 처리
 *
 * 폴링 방식 대비 장점:
 * - 즉시 처리 (폴링 지연 없음)
 * - 이벤트 순서 보장 (Kafka 파티션 기반)
 * - 주문 만료와의 경쟁 조건 제거
 *
 * Debezium Outbox Event Router 메시지 형식:
 * - Key: aggregate_id
 * - Value: payload (JSON)
 * - Headers: eventId, eventType, aggregateType
 */
@Component
class OutboxCdcConsumer(
    private val eventHandlerRegistry: EventHandlerRegistry,
    private val outboxEventService: OutboxEventService,
    private val dlqService: DlqService
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    @KafkaListener(
        topics = [
            Topics.Events.ORDER,
            Topics.Events.PAYMENT,
            Topics.Events.INVENTORY,
            Topics.Events.COUPON,
            Topics.Events.POINT,
            Topics.Events.DELIVERY,
            Topics.Events.CART
        ],
        groupId = "#{@kafkaProperties.consumer.outboxCdcGroupId}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        val eventId = extractHeader(record, "eventId")?.toLongOrNull()
        val eventType = extractHeader(record, "eventType")
        val aggregateType = extractHeader(record, "aggregateType")
        val aggregateId = record.key()
        val payload = extractPayload(record.value())

        if (eventType == null || aggregateType == null) {
            logger.warn(
                "[OutboxCdcConsumer] 필수 헤더 누락: topic={}, offset={}, eventType={}, aggregateType={}",
                record.topic(), record.offset(), eventType, aggregateType
            )
            acknowledgment.acknowledge()
            return
        }

        logger.debug(
            "[OutboxCdcConsumer] 이벤트 수신: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
            eventId, eventType, aggregateType, aggregateId
        )

        try {
            // OutboxEvent 객체 재구성
            val event = OutboxEvent(
                id = eventId ?: 0,
                eventType = eventType,
                aggregateType = aggregateType,
                aggregateId = aggregateId ?: "",
                payload = payload ?: ""
            )

            val handlers = eventHandlerRegistry.getHandlers(eventType)

            if (handlers.isEmpty()) {
                logger.warn("[OutboxCdcConsumer] 핸들러 없음: eventType={}", eventType)
                acknowledgment.acknowledge()
                return
            }

            var allSuccess = true
            val failedHandlers = mutableListOf<String>()

            handlers.forEach { handler ->
                try {
                    val success = handler.handle(event)
                    if (!success) {
                        allSuccess = false
                        failedHandlers.add(handler::class.simpleName ?: "Unknown")
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    failedHandlers.add(handler::class.simpleName ?: "Unknown")
                    logger.error(
                        "[OutboxCdcConsumer] 핸들러 예외: handler={}, eventType={}, error={}",
                        handler::class.simpleName, eventType, e.message, e
                    )
                }
            }

            if (allSuccess) {
                // DB에서 처리 완료 마킹
                eventId?.let { outboxEventService.markAsProcessed(it) }
                logger.info(
                    "[OutboxCdcConsumer] 이벤트 처리 완료: eventId={}, eventType={}",
                    eventId, eventType
                )
            } else {
                logger.warn(
                    "[OutboxCdcConsumer] 일부 핸들러 실패: eventId={}, failedHandlers={}",
                    eventId, failedHandlers
                )
                // 실패 시 재시도를 위해 예외 발생
                throw RuntimeException("핸들러 처리 실패: ${failedHandlers.joinToString(", ")}")
            }

            acknowledgment.acknowledge()

        } catch (e: Exception) {
            logger.error(
                "[OutboxCdcConsumer] 처리 실패: eventId={}, eventType={}, error={}",
                eventId, eventType, e.message, e
            )
            // 예외 발생 시 Kafka가 재시도하도록 acknowledge 하지 않음
            throw e
        }
    }

    private fun extractHeader(record: ConsumerRecord<String, String>, headerName: String): String? {
        return record.headers()
            .lastHeader(headerName)
            ?.value()
            ?.let { String(it) }
    }

    /**
     * Debezium JsonConverter 메시지에서 실제 payload 추출
     *
     * Debezium이 보내는 형식: {"schema":..., "payload":"실제 데이터"}
     * 이 함수는 payload 필드의 값을 추출
     */
    private fun extractPayload(rawValue: String?): String {
        if (rawValue.isNullOrBlank()) return ""

        return try {
            val jsonElement = json.parseToJsonElement(rawValue)
            val payloadElement = jsonElement.jsonObject["payload"]

            when {
                payloadElement == null -> rawValue
                payloadElement.toString().startsWith("\"") -> payloadElement.jsonPrimitive.content
                else -> payloadElement.toString()
            }
        } catch (e: Exception) {
            logger.warn("[OutboxCdcConsumer] payload 추출 실패, 원본 사용: {}", e.message)
            rawValue
        }
    }
}
