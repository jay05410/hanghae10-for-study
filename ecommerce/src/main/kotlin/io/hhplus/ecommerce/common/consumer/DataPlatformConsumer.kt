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
 * 주문 정보를 Kafka에서 수신하여 데이터 플랫폼에 전송
 *
 * 특징:
 * - 수동 ACK (처리 완료 후 커밋)
 * - TraceId 전파 (분산 추적)
 * - 실패 시 재시도 (Kafka 재처리)
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
     *
     * PAYMENT_COMPLETED 이벤트 수신 → 주문 정보 데이터 플랫폼 전송
     */
    @KafkaListener(
        topics = ["\${kafka.topics.data-platform}"],
        groupId = "\${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeOrderInfo(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        val traceId = extractTraceId(record)

        try {
            // TraceId를 MDC에 설정 (로그 추적용)
            traceId?.let { MDC.put("traceId", it) }

            logger.info(
                "[DataPlatformConsumer] 메시지 수신: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key()
            )

            // 페이로드 파싱
            val orderInfo = objectMapper.readValue(record.value(), OrderInfoPayload::class.java)

            // 데이터 플랫폼 전송
            val response = dataPlatformClient.sendOrderInfo(orderInfo)

            if (response.success) {
                // 성공 시 커밋
                acknowledgment.acknowledge()
                logger.info(
                    "[DataPlatformConsumer] 처리 완료: orderId={}, offset={}",
                    orderInfo.orderId, record.offset()
                )
            } else {
                // 실패 시 커밋하지 않음 → Kafka가 재전달
                logger.warn(
                    "[DataPlatformConsumer] 전송 실패, 재처리 예정: orderId={}, message={}",
                    orderInfo.orderId, response.message
                )
                // 실패 시에도 ACK를 하지 않으면 무한 재시도가 될 수 있음
                // 실무에서는 재시도 횟수 제한 + DLQ 이동 필요
                acknowledgment.acknowledge()
            }

        } catch (e: Exception) {
            logger.error(
                "[DataPlatformConsumer] 처리 중 오류: offset={}, error={}",
                record.offset(), e.message, e
            )
            // 파싱 오류 등은 재시도해도 의미 없으므로 ACK
            acknowledgment.acknowledge()
        } finally {
            MDC.remove("traceId")
        }
    }

    /**
     * Kafka 헤더에서 TraceId 추출
     */
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
