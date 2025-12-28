package io.hhplus.ecommerce.checkout.application.consumer

import io.hhplus.ecommerce.checkout.application.service.CheckoutQueueService
import io.hhplus.ecommerce.checkout.application.usecase.CheckoutUseCase
import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutItem
import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutRequest
import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.common.outbox.payload.CheckoutQueuePayload
import io.hhplus.ecommerce.common.outbox.payload.CheckoutResultPayload
import io.hhplus.ecommerce.common.sse.SseEmitterService
import io.hhplus.ecommerce.common.sse.SseEventType
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * 체크아웃 큐 Consumer
 *
 * Kafka 파티션 기반 순차 처리
 * - 동일 파티션 키(상품/사용자)는 순서 보장
 * - 분산락/DB락 없이 순차 처리로 락 경합 제거
 */
@Component
class CheckoutQueueConsumer(
    private val checkoutUseCase: CheckoutUseCase,
    private val checkoutQueueService: CheckoutQueueService,
    private val sseEmitterService: SseEmitterService
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    @KafkaListener(
        topics = [Topics.Queue.CHECKOUT],
        groupId = "checkout-queue-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        val startTime = System.currentTimeMillis()
        var payload: CheckoutQueuePayload? = null

        try {
            payload = json.decodeFromString<CheckoutQueuePayload>(record.value())

            logger.info {
                "[CheckoutConsumer] 처리 시작: requestId=${payload.requestId}, " +
                    "userId=${payload.userId}, isCart=${payload.isFromCart()}, " +
                    "position=${payload.queuePosition}"
            }

            // Payload → CheckoutRequest 변환
            val checkoutRequest = CheckoutRequest(
                userId = payload.userId,
                cartItemIds = payload.cartItemIds,
                items = payload.items?.map { item ->
                    CheckoutItem(
                        productId = item.productId,
                        quantity = item.quantity,
                        giftWrap = item.giftWrap,
                        giftMessage = item.giftMessage
                    )
                }
            )

            // 체크아웃 실행 (락 없이 - Kafka 파티션이 순서 보장)
            val checkoutSession = checkoutUseCase.processCheckout(checkoutRequest)

            // 성공 결과 SSE 전송
            val result = CheckoutResultPayload(
                requestId = payload.requestId,
                success = true,
                orderId = checkoutSession.orderId,
                orderNumber = checkoutSession.orderNumber,
                finalAmount = checkoutSession.finalAmount,
                queuePosition = payload.queuePosition
            )

            sseEmitterService.sendEvent(
                userId = payload.userId,
                eventType = SseEventType.CHECKOUT_COMPLETED,
                data = result
            )

            val processingTime = System.currentTimeMillis() - startTime
            logger.info {
                "[CheckoutConsumer] 완료: requestId=${payload.requestId}, " +
                    "orderId=${checkoutSession.orderId}, time=${processingTime}ms"
            }

        } catch (e: Exception) {
            logger.error(e) {
                "[CheckoutConsumer] 실패: requestId=${payload?.requestId}, error=${e.message}"
            }

            payload?.let {
                val result = CheckoutResultPayload(
                    requestId = it.requestId,
                    success = false,
                    errorMessage = e.message ?: "체크아웃 처리 중 오류",
                    queuePosition = it.queuePosition
                )

                sseEmitterService.sendEvent(
                    userId = it.userId,
                    eventType = SseEventType.CHECKOUT_FAILED,
                    data = result
                )
            }
        } finally {
            payload?.let {
                checkoutQueueService.removeQueuePosition(it.requestId)
            }
            acknowledgment.acknowledge()
        }
    }
}
