package io.hhplus.ecommerce.order.application

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.order.domain.entity.OrderQueueRequest
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.exception.OrderException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 주문 생성 Queue 서비스
 *
 * 역할:
 * - Redis를 사용한 주문 생성 Queue 관리
 * - 대기열 등록, 조회, 처리 상태 업데이트
 *
 * Redis 자료구조:
 * 1. Queue (List): "order:queue:waiting"
 *    - 대기 중인 queueId 목록 (FIFO)
 *    - LPUSH로 추가, RPOP으로 꺼냄
 *
 * 2. Request Data (String): "order:queue:request:{queueId}"
 *    - JSON 직렬화된 OrderQueueRequest 전체 데이터
 *    - TTL: 1시간 (처리 완료 후 자동 삭제)
 *
 * 3. User Mapping (String): "order:queue:user:{userId}"
 *    - userId → queueId 매핑 (중복 방지)
 *    - TTL: 1시간
 *
 * 4. Queue Position Counter (String): "order:queue:position"
 *    - Queue 순번 카운터
 *    - INCR로 자동 증가
 */
@Service
class OrderQueueService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val snowflakeGenerator: SnowflakeGenerator
) {

    companion object {
        private const val QUEUE_KEY = "order:queue:waiting"
        private const val REQUEST_KEY_PREFIX = "order:queue:request"
        private const val USER_MAPPING_KEY_PREFIX = "order:queue:user"
        private const val POSITION_COUNTER_KEY = "order:queue:position"
        private const val DEFAULT_TTL_HOURS = 1L
    }

    /**
     * 주문 생성 요청을 Queue에 등록
     *
     * @param request 주문 생성 요청
     * @return 등록된 Queue 요청
     * @throws OrderException.AlreadyInQueue 이미 Queue에 등록된 경우
     */
    fun enqueue(request: CreateOrderRequest): OrderQueueRequest {
        val userMappingKey = getUserMappingKey(request.userId)

        // 중복 체크: 이미 Queue에 등록되어 있는지 확인
        val existingQueueId = redisTemplate.opsForValue().get(userMappingKey) as? String
        if (existingQueueId != null) {
            val existingRequest = getQueueRequest(existingQueueId)
            if (existingRequest != null && existingRequest.isActive()) {
                throw OrderException.AlreadyInOrderQueue(request.userId)
            }
        }

        // Queue 순번 생성 (원자적 증가)
        val queuePosition = getNextQueuePosition()

        // Snowflake ID 생성
        val queueId = snowflakeGenerator.nextId().toString()

        // Queue 요청 생성
        val queueRequest = OrderQueueRequest.create(
            queueId = queueId,
            userId = request.userId,
            items = request.items,
            deliveryAddress = request.deliveryAddress,
            usedCouponId = request.usedCouponId,
            queuePosition = queuePosition
        )

        // 1. Queue에 queueId 추가 (FIFO)
        redisTemplate.opsForList().leftPush(QUEUE_KEY, queueRequest.queueId)

        // 2. Request 데이터 저장
        saveQueueRequest(queueRequest)

        // 3. User Mapping 저장 (중복 방지용)
        redisTemplate.opsForValue().set(
            userMappingKey,
            queueRequest.queueId,
            DEFAULT_TTL_HOURS,
            TimeUnit.HOURS
        )

        return queueRequest
    }

    /**
     * Queue에서 다음 요청을 꺼냄 (Worker용)
     *
     * @return 다음 처리할 Queue 요청 (없으면 null)
     */
    fun dequeue(): OrderQueueRequest? {
        // Queue에서 queueId 꺼내기 (RPOP)
        val queueId = redisTemplate.opsForList().rightPop(QUEUE_KEY) as? String
            ?: return null

        // Request 데이터 조회
        val queueRequest = getQueueRequest(queueId)
            ?: return null

        // 상태를 PROCESSING으로 변경
        queueRequest.startProcessing()
        saveQueueRequest(queueRequest)

        return queueRequest
    }

    /**
     * Queue 요청 상태를 조회
     *
     * @param queueId Queue ID
     * @return Queue 요청 (없으면 null)
     */
    fun getQueueRequest(queueId: String): OrderQueueRequest? {
        val requestKey = getRequestKey(queueId)
        val jsonData = redisTemplate.opsForValue().get(requestKey) as? String
            ?: return null

        return objectMapper.readValue(jsonData, OrderQueueRequest::class.java)
    }

    /**
     * Queue 요청을 저장/업데이트
     *
     * @param queueRequest 저장할 Queue 요청
     */
    fun saveQueueRequest(queueRequest: OrderQueueRequest) {
        val requestKey = getRequestKey(queueRequest.queueId)
        val jsonData = objectMapper.writeValueAsString(queueRequest)

        redisTemplate.opsForValue().set(
            requestKey,
            jsonData,
            DEFAULT_TTL_HOURS,
            TimeUnit.HOURS
        )
    }

    /**
     * Queue 요청을 완료 처리
     *
     * @param queueId Queue ID
     * @param orderId 생성된 주문 ID
     */
    fun completeQueue(queueId: String, orderId: Long) {
        val queueRequest = getQueueRequest(queueId)
            ?: return

        queueRequest.complete(orderId)
        saveQueueRequest(queueRequest)
    }

    /**
     * Queue 요청을 실패 처리
     *
     * @param queueId Queue ID
     * @param reason 실패 사유
     */
    fun failQueue(queueId: String, reason: String) {
        val queueRequest = getQueueRequest(queueId)
            ?: return

        queueRequest.fail(reason)
        saveQueueRequest(queueRequest)
    }

    /**
     * 현재 Queue 크기를 조회
     *
     * @return Queue에 대기 중인 요청 수
     */
    fun getQueueSize(): Long {
        return redisTemplate.opsForList().size(QUEUE_KEY) ?: 0L
    }

    /**
     * 사용자의 Queue 요청을 조회
     *
     * @param userId 사용자 ID
     * @return Queue 요청 (없으면 null)
     */
    fun getUserQueueRequest(userId: Long): OrderQueueRequest? {
        val userMappingKey = getUserMappingKey(userId)
        val queueId = redisTemplate.opsForValue().get(userMappingKey) as? String
            ?: return null

        return getQueueRequest(queueId)
    }

    /**
     * 처리 상태별 Queue 요청 수 조회
     *
     * @return 상태별 카운트 맵
     */
    fun getQueueStatusCount(): Map<String, Int> {
        val requestKeys = redisTemplate.keys("$REQUEST_KEY_PREFIX:*")
        val statusCount = mutableMapOf<String, Int>()

        requestKeys.forEach { key ->
            try {
                val jsonData = redisTemplate.opsForValue().get(key) as? String
                if (jsonData != null) {
                    val request = objectMapper.readValue(jsonData, OrderQueueRequest::class.java)
                    val status = request.status.name
                    statusCount[status] = statusCount.getOrDefault(status, 0) + 1
                }
            } catch (e: Exception) {
                // 파싱 실패시 무시
            }
        }

        return statusCount
    }

    /**
     * 다음 Queue 순번을 생성 (원자적 증가)
     *
     * @return 새로운 Queue 순번
     */
    private fun getNextQueuePosition(): Int {
        val position = redisTemplate.opsForValue().increment(POSITION_COUNTER_KEY)
            ?: throw IllegalStateException("Queue 순번 생성 실패")

        return position.toInt()
    }

    /**
     * 전체 Queue 데이터를 삭제 (테스트용)
     *
     * 주의: 이 메서드는 테스트 환경에서만 사용해야 함
     */
    fun clearAllQueueData() {
        // 1. Queue 목록 삭제
        redisTemplate.delete(QUEUE_KEY)

        // 2. Position 카운터 삭제
        redisTemplate.delete(POSITION_COUNTER_KEY)

        // 3. 모든 request 키 삭제
        val requestKeys = redisTemplate.keys("$REQUEST_KEY_PREFIX:*")
        if (requestKeys.isNotEmpty()) {
            redisTemplate.delete(requestKeys)
        }

        // 4. 모든 user mapping 키 삭제
        val userMappingKeys = redisTemplate.keys("$USER_MAPPING_KEY_PREFIX:*")
        if (userMappingKeys.isNotEmpty()) {
            redisTemplate.delete(userMappingKeys)
        }
    }

    // Redis Key 생성 메서드들
    private fun getRequestKey(queueId: String): String = "$REQUEST_KEY_PREFIX:$queueId"
    private fun getUserMappingKey(userId: Long): String = "$USER_MAPPING_KEY_PREFIX:$userId"
}