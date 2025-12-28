package io.hhplus.ecommerce.checkout.application.service

import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutRequest
import io.hhplus.ecommerce.common.messaging.KafkaMessagePublisher
import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.common.outbox.payload.CheckoutItemPayload
import io.hhplus.ecommerce.common.outbox.payload.CheckoutQueuePayload
import io.hhplus.ecommerce.common.sse.SseEmitterService
import io.hhplus.ecommerce.common.sse.SseEventType
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 체크아웃 큐 서비스
 *
 * 체크아웃 = 결제 버튼 클릭 → 재고 확보 시점
 * Kafka 큐로 순차 처리하여 DB 락 경합 제거
 *
 * 처리 흐름:
 * 1. 요청: Kafka 큐 등록 + 대기열 순번 반환
 * 2. 비동기: Consumer 순차 처리
 * 3. 완료: SSE로 결과 푸시
 */
@Service
class CheckoutQueueService(
    private val kafkaPublisher: KafkaMessagePublisher,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val sseEmitterService: SseEmitterService
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val QUEUE_COUNTER_KEY = "ecom:checkout:queue:counter"
        private const val QUEUE_POSITION_PREFIX = "ecom:checkout:queue:position:"
        private const val QUEUE_TTL_HOURS = 1L
    }

    /**
     * 체크아웃 요청을 큐에 등록
     */
    fun enqueue(request: CheckoutRequest): CheckoutQueueResponse {
        val requestId = UUID.randomUUID().toString()

        // 파티션 키 결정: 장바구니는 userId, 바로주문은 첫 상품 ID
        val partitionKey = if (request.isFromCart()) {
            "user:${request.userId}"
        } else {
            "product:${request.items!!.first().productId}"
        }

        val queuePosition = getNextQueuePosition(partitionKey)

        val payload = CheckoutQueuePayload(
            requestId = requestId,
            userId = request.userId,
            cartItemIds = request.cartItemIds,
            items = request.items?.map { item ->
                CheckoutItemPayload(
                    productId = item.productId,
                    quantity = item.quantity,
                    giftWrap = item.giftWrap,
                    giftMessage = item.giftMessage
                )
            },
            queuePosition = queuePosition,
            requestedAt = System.currentTimeMillis()
        )

        // Kafka 큐에 발행
        kafkaPublisher.publish(
            topic = Topics.Queue.CHECKOUT,
            key = partitionKey,
            payload = mapOf(
                "requestId" to payload.requestId,
                "userId" to payload.userId,
                "cartItemIds" to payload.cartItemIds,
                "items" to payload.items?.map { mapOf(
                    "productId" to it.productId,
                    "quantity" to it.quantity,
                    "giftWrap" to it.giftWrap,
                    "giftMessage" to it.giftMessage
                )},
                "queuePosition" to payload.queuePosition,
                "requestedAt" to payload.requestedAt
            )
        )

        saveQueuePosition(requestId, queuePosition)

        logger.info {
            "[CheckoutQueue] 등록: requestId=$requestId, userId=${request.userId}, " +
                "isCart=${request.isFromCart()}, position=$queuePosition"
        }

        sseEmitterService.sendEvent(
            userId = request.userId,
            eventType = SseEventType.CHECKOUT_QUEUED,
            data = mapOf(
                "requestId" to requestId,
                "queuePosition" to queuePosition,
                "message" to "체크아웃 대기열 등록. 순번: $queuePosition"
            )
        )

        return CheckoutQueueResponse(
            requestId = requestId,
            queuePosition = queuePosition,
            estimatedWaitSeconds = queuePosition * 2
        )
    }

    /**
     * 대기열 순번 조회
     */
    fun getQueuePosition(requestId: String): Int? {
        val key = "$QUEUE_POSITION_PREFIX$requestId"
        return (redisTemplate.opsForValue().get(key) as? String)?.toIntOrNull()
    }

    /**
     * 다음 대기열 순번 발급
     */
    private fun getNextQueuePosition(partitionKey: String): Int {
        val key = "$QUEUE_COUNTER_KEY:$partitionKey"
        val position = redisTemplate.opsForValue().increment(key) ?: 1L
        redisTemplate.expire(key, QUEUE_TTL_HOURS, TimeUnit.HOURS)
        return position.toInt()
    }

    /**
     * 요청 ID - 순번 매핑 저장
     */
    private fun saveQueuePosition(requestId: String, position: Int) {
        val key = "$QUEUE_POSITION_PREFIX$requestId"
        redisTemplate.opsForValue().set(key, position.toString(), QUEUE_TTL_HOURS, TimeUnit.HOURS)
    }

    /**
     * 대기열 순번 삭제 (처리 완료 시)
     */
    fun removeQueuePosition(requestId: String) {
        val key = "$QUEUE_POSITION_PREFIX$requestId"
        redisTemplate.delete(key)
    }
}

/**
 * 체크아웃 큐 등록 응답
 */
data class CheckoutQueueResponse(
    val requestId: String,
    val queuePosition: Int,
    val estimatedWaitSeconds: Int
)
