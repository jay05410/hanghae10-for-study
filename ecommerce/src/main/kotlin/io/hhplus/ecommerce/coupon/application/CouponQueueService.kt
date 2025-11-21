package io.hhplus.ecommerce.coupon.application

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.exception.CouponException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 쿠폰 발급 Queue 서비스
 *
 * 역할:
 * - Redis를 사용한 쿠폰 발급 Queue 관리
 * - 대기열 등록, 조회, 처리 상태 업데이트
 *
 * Redis 자료구조:
 * 1. Queue (List): "coupon:queue:waiting:{couponId}"
 *    - 대기 중인 queueId 목록 (FIFO)
 *    - LPUSH로 추가, RPOP으로 꺼냄
 *
 * 2. Request Data (String): "coupon:queue:request:{queueId}"
 *    - JSON 직렬화된 CouponQueueRequest 전체 데이터
 *    - TTL: 1시간 (처리 완료 후 자동 삭제)
 *
 * 3. User Mapping (String): "coupon:queue:user:{userId}:{couponId}"
 *    - userId+couponId → queueId 매핑 (중복 방지)
 *    - TTL: 1시간
 *
 * 4. Queue Position Counter (String): "coupon:queue:position:{couponId}"
 *    - 쿠폰별 Queue 순번 카운터
 *    - INCR로 자동 증가
 */
@Service
class CouponQueueService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val snowflakeGenerator: SnowflakeGenerator,
    private val couponRepository: CouponRepository
) {

    companion object {
        private const val QUEUE_KEY_PREFIX = "coupon:queue:waiting"
        private const val REQUEST_KEY_PREFIX = "coupon:queue:request"
        private const val USER_MAPPING_KEY_PREFIX = "coupon:queue:user"
        private const val POSITION_COUNTER_KEY_PREFIX = "coupon:queue:position"
        private const val DEFAULT_TTL_HOURS = 1L
    }

    /**
     * 쿠폰 발급 요청을 Queue에 등록
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @param couponName 쿠폰명
     * @return 등록된 Queue 요청
     * @throws CouponException.AlreadyInQueue 이미 Queue에 등록된 경우
     */
    fun enqueue(userId: Long, couponId: Long, couponName: String): CouponQueueRequest {
        val userMappingKey = getUserMappingKey(userId, couponId)

        // 중복 체크: 이미 Queue에 등록되어 있는지 확인
        val existingQueueId = redisTemplate.opsForValue().get(userMappingKey) as? String
        if (existingQueueId != null) {
            val existingRequest = getQueueRequest(existingQueueId)
            if (existingRequest != null && existingRequest.isActive()) {
                throw CouponException.AlreadyInQueue(userId, couponName)
            }
        }

        // 쿠폰 정보 조회 및 큐 크기 제한 체크
        val coupon = couponRepository.findById(couponId)
            ?: throw CouponException.CouponNotFound(couponId)

        // Snowflake ID 생성
        val queueId = snowflakeGenerator.nextId().toString()

        // Redis Lua 스크립트로 원자적 큐 크기 제한
        val queueKey = getQueueKey(couponId)

        // 원자적 큐 크기 제한을 위한 Lua 스크립트 (카운터 기반)
        val luaScript = """
            local queue_key = KEYS[1]
            local counter_key = KEYS[2]
            local max_size = tonumber(ARGV[1])
            local queue_id = ARGV[2]

            local current_count = redis.call('INCR', counter_key)

            if current_count > max_size then
                redis.call('DECR', counter_key)
                return 0
            end

            redis.call('LPUSH', queue_key, queue_id)
            return 1
        """.trimIndent()

        val counterKey = "coupon:queue:counter:$couponId"

        val result = try {
            redisTemplate.execute { connection ->
                connection.eval(
                    luaScript.toByteArray(),
                    org.springframework.data.redis.connection.ReturnType.INTEGER,
                    2,
                    queueKey.toByteArray(),
                    counterKey.toByteArray(),
                    coupon.totalQuantity.toString().toByteArray(),
                    queueId.toByteArray()
                ) as? Long ?: 0L
            }
        } catch (e: Exception) {
            println("Lua script execution error for user $userId: ${e.message}")
            e.printStackTrace()
            0L
        }

        println("Lua script result for user $userId: $result, maxSize: ${coupon.totalQuantity}, queueKey: $queueKey")

        if (result != 1L) {
            throw CouponException.CouponSoldOut(couponName, 0)
        }

        // Lua 스크립트 성공 후 실제 큐 크기 확인
        val actualQueueSize = redisTemplate.opsForList().size(queueKey) ?: 0L
        println("Queue 등록 후 실제 큐 크기 확인 - userId: $userId, queueKey: $queueKey, size: $actualQueueSize")

        // Queue 순번 생성 (원자적 증가)
        val queuePosition = try {
            getNextQueuePosition(couponId)
        } catch (e: Exception) {
            println("Queue 순번 생성 실패 - userId: $userId, error: ${e.message}")
            throw e
        }

        // Queue 요청 생성
        val queueRequest = CouponQueueRequest.create(
            queueId = queueId,
            userId = userId,
            couponId = couponId,
            couponName = couponName,
            queuePosition = queuePosition
        )

        // Queue 데이터 저장
        try {
            saveQueueRequest(queueRequest)
            println("Queue 데이터 저장 성공 - userId: $userId, queueId: $queueId")
        } catch (e: Exception) {
            println("Queue 데이터 저장 실패 - userId: $userId, error: ${e.message}")
            throw e
        }

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
     * @param couponId 쿠폰 ID
     * @return 다음 처리할 Queue 요청 (없으면 null)
     */
    fun dequeue(couponId: Long): CouponQueueRequest? {
        val queueKey = getQueueKey(couponId)

        // Queue에서 queueId 꺼내기 (RPOP)
        val queueId = redisTemplate.opsForList().rightPop(queueKey) as? String
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
    fun getQueueRequest(queueId: String): CouponQueueRequest? {
        val requestKey = getRequestKey(queueId)
        val jsonData = redisTemplate.opsForValue().get(requestKey) as? String
            ?: return null

        return objectMapper.readValue(jsonData, CouponQueueRequest::class.java)
    }

    /**
     * Queue 요청을 저장/업데이트
     *
     * @param queueRequest 저장할 Queue 요청
     */
    fun saveQueueRequest(queueRequest: CouponQueueRequest) {
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
     * @param userCouponId 발급된 사용자 쿠폰 ID
     */
    fun completeQueue(queueId: String, userCouponId: Long) {
        val queueRequest = getQueueRequest(queueId)
            ?: return

        queueRequest.complete(userCouponId)
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
     * 특정 쿠폰의 현재 Queue 크기를 조회
     *
     * @param couponId 쿠폰 ID
     * @return Queue에 대기 중인 요청 수
     */
    fun getQueueSize(couponId: Long): Long {
        val queueKey = getQueueKey(couponId)
        return redisTemplate.opsForList().size(queueKey) ?: 0L
    }

    /**
     * 사용자의 Queue 요청을 조회
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return Queue 요청 (없으면 null)
     */
    fun getUserQueueRequest(userId: Long, couponId: Long): CouponQueueRequest? {
        val userMappingKey = getUserMappingKey(userId, couponId)
        val queueId = redisTemplate.opsForValue().get(userMappingKey) as? String
            ?: return null

        return getQueueRequest(queueId)
    }

    /**
     * 다음 Queue 순번을 생성 (원자적 증가)
     *
     * @param couponId 쿠폰 ID
     * @return 새로운 Queue 순번
     */
    private fun getNextQueuePosition(couponId: Long): Int {
        val counterKey = getPositionCounterKey(couponId)
        val position = redisTemplate.opsForValue().increment(counterKey)
            ?: throw IllegalStateException("Queue 순번 생성 실패")

        return position.toInt()
    }

    // Redis Key 생성 메서드들
    private fun getQueueKey(couponId: Long): String = "$QUEUE_KEY_PREFIX:$couponId"
    private fun getRequestKey(queueId: String): String = "$REQUEST_KEY_PREFIX:$queueId"
    private fun getUserMappingKey(userId: Long, couponId: Long): String =
        "$USER_MAPPING_KEY_PREFIX:$userId:$couponId"
    private fun getPositionCounterKey(couponId: Long): String = "$POSITION_COUNTER_KEY_PREFIX:$couponId"
}
