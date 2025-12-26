package io.hhplus.ecommerce.checkout.application.service

import io.hhplus.ecommerce.common.messaging.KafkaMessagePublisher
import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.common.outbox.payload.CheckoutQueuePayload
import io.hhplus.ecommerce.common.sse.SseEmitterService
import io.hhplus.ecommerce.common.sse.SseEventType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 선착순 체크아웃 큐 서비스
 *
 * 재고가 제한된 인기 상품의 동시 주문 시 DB 락 경합을 제거
 *
 * 처리 흐름:
 * 1. 요청 즉시: Kafka 큐에 등록 + 대기열 순번 반환
 * 2. 비동기: CheckoutQueueConsumer가 순차 처리
 * 3. 완료 시: SSE로 결과 푸시
 */
@Service
class CheckoutQueueService(
    private val kafkaPublisher: KafkaMessagePublisher,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val sseEmitterService: SseEmitterService
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val QUEUE_COUNTER_KEY = "ecom:checkout:queue:counter"
        private const val QUEUE_POSITION_PREFIX = "ecom:checkout:queue:position:"
        private const val QUEUE_TTL_HOURS = 1L
    }

    /**
     * 체크아웃 요청을 큐에 등록
     *
     * @return 요청 ID와 대기열 순번
     */
    fun enqueue(
        userId: Long,
        productId: Long,
        quantity: Int
    ): CheckoutQueueResponse {
        val requestId = UUID.randomUUID().toString()
        val queuePosition = getNextQueuePosition(productId)

        val payload = CheckoutQueuePayload(
            requestId = requestId,
            userId = userId,
            productId = productId,
            quantity = quantity,
            queuePosition = queuePosition,
            requestedAt = System.currentTimeMillis()
        )

        // Kafka 큐에 발행 (상품 ID를 키로 사용하여 파티션 보장)
        // KafkaMessagePublisher가 Jackson으로 직렬화하므로 payload 객체 직접 전달
        kafkaPublisher.publish(
            topic = Topics.CHECKOUT_QUEUE,
            key = productId.toString(),
            payload = mapOf(
                "requestId" to payload.requestId,
                "userId" to payload.userId,
                "productId" to payload.productId,
                "quantity" to payload.quantity,
                "queuePosition" to payload.queuePosition,
                "requestedAt" to payload.requestedAt
            )
        )

        // 요청 ID - 순번 매핑 저장 (상태 조회용)
        saveQueuePosition(requestId, queuePosition)

        logger.info {
            "[CheckoutQueue] 대기열 등록: requestId=$requestId, userId=$userId, " +
                "productId=$productId, position=$queuePosition"
        }

        // SSE로 대기열 등록 알림
        sseEmitterService.sendEvent(
            userId = userId,
            eventType = SseEventType.CHECKOUT_QUEUED,
            data = mapOf(
                "requestId" to requestId,
                "queuePosition" to queuePosition,
                "productId" to productId,
                "message" to "대기열에 등록되었습니다. 순번: $queuePosition"
            )
        )

        return CheckoutQueueResponse(
            requestId = requestId,
            queuePosition = queuePosition,
            estimatedWaitSeconds = queuePosition * 2  // 예상 대기 시간 (건당 2초 가정)
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
     * 다음 대기열 순번 발급 (상품별)
     */
    private fun getNextQueuePosition(productId: Long): Int {
        val key = "$QUEUE_COUNTER_KEY:$productId"
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
