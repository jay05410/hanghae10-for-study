package io.hhplus.ecommerce.order.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.order.application.mapper.toOrderInfoPayload
import io.hhplus.ecommerce.common.messaging.MessagePublisher
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.order.application.usecase.GetOrderQueryUseCase
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 주문 정보 데이터 플랫폼 전송 핸들러
 *
 * PAYMENT_COMPLETED 이벤트 수신 시:
 * 1. 주문 정보 조회
 * 2. Kafka 토픽으로 발행 (데이터 플랫폼용)
 *
 * 트랜잭션 분리:
 * - 메인 트랜잭션: 결제 완료 → Outbox 저장
 * - 이 핸들러: Outbox 폴링 → Kafka 발행 (별도 트랜잭션)
 * - Kafka Consumer: 데이터 플랫폼 전송 (완전 분리)
 *
 * 이점:
 * - 데이터 플랫폼 전송 실패가 결제에 영향 없음
 * - 비동기 처리로 응답 시간 개선
 * - Kafka 기반으로 재시도 및 순서 보장
 */
@Component
class OrderDataPlatformHandler(
    private val messagePublisher: MessagePublisher,
    private val getOrderQueryUseCase: GetOrderQueryUseCase,
    private val objectMapper: ObjectMapper,
    @Value("\${kafka.topics.data-platform:ecommerce.data-platform}")
    private val dataPlatformTopic: String
) : EventHandler {

    private val logger = KotlinLogging.logger {}

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.PAYMENT_COMPLETED)
    }

    override fun handle(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()
            val paymentId = (payload["paymentId"] as? Number)?.toLong()

            logger.info(
                "[OrderDataPlatformHandler] 데이터 플랫폼 전송 시작: orderId={}, paymentId={}",
                orderId, paymentId
            )

            // 주문 정보 조회
            val orderWithItems = getOrderQueryUseCase.getOrderWithItems(orderId)

            if (orderWithItems == null) {
                logger.warn("[OrderDataPlatformHandler] 주문 정보를 찾을 수 없습니다: orderId={}", orderId)
                return false
            }

            val (order, items) = orderWithItems

            // Kafka 토픽으로 발행 (확장 함수 사용)
            messagePublisher.publish(
                topic = dataPlatformTopic,
                key = orderId.toString(),
                payload = order.toOrderInfoPayload(items, paymentId)
            )

            logger.info(
                "[OrderDataPlatformHandler] 데이터 플랫폼 전송 완료: orderId={}, topic={}",
                orderId, dataPlatformTopic
            )

            true
        } catch (e: Exception) {
            logger.error(
                "[OrderDataPlatformHandler] 데이터 플랫폼 전송 실패: eventId={}, error={}",
                event.id, e.message, e
            )
            false
        }
    }
}
