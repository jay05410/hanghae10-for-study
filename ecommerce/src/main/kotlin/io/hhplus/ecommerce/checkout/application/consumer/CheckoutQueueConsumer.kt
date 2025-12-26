package io.hhplus.ecommerce.checkout.application.consumer

import io.hhplus.ecommerce.checkout.application.service.CheckoutQueueService
import io.hhplus.ecommerce.checkout.application.usecase.CheckoutUseCase
import io.hhplus.ecommerce.checkout.presentation.dto.DirectOrderItem
import io.hhplus.ecommerce.checkout.presentation.dto.InitiateCheckoutRequest
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
 * 선착순 체크아웃 큐 Consumer
 *
 * Kafka 파티션 기반으로 상품별 순차 처리
 * - 동일 상품(productId)은 같은 파티션으로 라우팅 → 순서 보장
 * - DB 락 경합 없이 순차 처리로 안정적인 재고 관리
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
        topics = [Topics.CHECKOUT_QUEUE],
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
                "[CheckoutQueueConsumer] 처리 시작: requestId=${payload.requestId}, " +
                    "userId=${payload.userId}, productId=${payload.productId}, " +
                    "position=${payload.queuePosition}"
            }

            // 체크아웃 실행
            val checkoutSession = checkoutUseCase.initiateCheckout(
                InitiateCheckoutRequest(
                    userId = payload.userId,
                    directOrderItems = listOf(
                        DirectOrderItem(
                            productId = payload.productId,
                            quantity = payload.quantity
                        )
                    )
                )
            )

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
                "[CheckoutQueueConsumer] 처리 완료: requestId=${payload.requestId}, " +
                    "orderId=${checkoutSession.orderId}, processingTime=${processingTime}ms"
            }

        } catch (e: Exception) {
            logger.error(e) {
                "[CheckoutQueueConsumer] 처리 실패: requestId=${payload?.requestId}, error=${e.message}"
            }

            // 실패 결과 SSE 전송
            payload?.let {
                val result = CheckoutResultPayload(
                    requestId = it.requestId,
                    success = false,
                    errorMessage = e.message ?: "체크아웃 처리 중 오류 발생",
                    queuePosition = it.queuePosition
                )

                sseEmitterService.sendEvent(
                    userId = it.userId,
                    eventType = SseEventType.CHECKOUT_FAILED,
                    data = result
                )
            }
        } finally {
            // 대기열 순번 정보 삭제
            payload?.let {
                checkoutQueueService.removeQueuePosition(it.requestId)
            }
            acknowledgment.acknowledge()
        }
    }
}
