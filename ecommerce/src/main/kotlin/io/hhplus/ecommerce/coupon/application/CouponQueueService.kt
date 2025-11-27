package io.hhplus.ecommerce.coupon.application

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.util.RedisUtil
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.coupon.exception.CouponException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

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
    private val snowflakeGenerator: SnowflakeGenerator
) {

    companion object {
        private const val QUEUE_KEY_PREFIX = "coupon:queue:waiting"
        private const val REQUEST_KEY_PREFIX = "coupon:queue:request"
        private const val USER_MAPPING_KEY_PREFIX = "coupon:queue:user"
        private const val POSITION_COUNTER_KEY_PREFIX = "coupon:queue:position"
        private const val DEFAULT_TTL_HOURS = 1L
    }


    /**
     * 쿠폰 발급 요청을 Queue에 등록 (큐 크기 제한 포함, 원자적 처리)
     *
     * @param userId 사용자 ID
     * @param coupon 쿠폰 정보
     * @return 등록된 Queue 요청
     * @throws CouponException.AlreadyInQueue 이미 Queue에 등록된 경우
     * @throws CouponException.QueueFull 큐가 가득 찬 경우
     */
    @DistributedLock(key = "coupon:enqueue:#{#coupon.id}")
    fun enqueueWithSizeLimit(userId: Long, coupon: Coupon): CouponQueueRequest {
        val userMappingKey = getUserMappingKey(userId, coupon.id)
        val queueKey = getQueueKey(coupon.id)

        // 중복 체크: 이미 Queue에 등록되어 있는지 확인
        val existingQueueId = RedisUtil.getValue(redisTemplate, userMappingKey)
        if (existingQueueId != null) {
            val existingRequest = getQueueRequest(existingQueueId)
            if (existingRequest != null && existingRequest.isActive()) {
                throw CouponException.AlreadyInQueue(userId, coupon.name)
            }
        }

        // Snowflake ID 생성
        val queueId = snowflakeGenerator.nextId().toString()

        // 원자적 큐 크기 체크 및 추가
        val enqueued = RedisUtil.enqueueWithSizeCheck(redisTemplate, queueKey, queueId, coupon.totalQuantity)
        if (!enqueued) {
            throw CouponException.QueueFull(coupon.name)
        }

        // Queue 순번 생성 (원자적 증가)
        val queuePosition = RedisUtil.incrementCounter(redisTemplate, getPositionCounterKey(coupon.id)).toInt()

        // Queue 요청 생성
        val queueRequest = CouponQueueRequest.create(
            queueId = queueId,
            userId = userId,
            couponId = coupon.id,
            couponName = coupon.name,
            queuePosition = queuePosition
        )

        // Queue 데이터 저장
        RedisUtil.saveJson(redisTemplate, objectMapper, getRequestKey(queueRequest.queueId), queueRequest, DEFAULT_TTL_HOURS)

        // User Mapping 저장 (중복 방지용)
        RedisUtil.setValue(redisTemplate, userMappingKey, queueRequest.queueId, DEFAULT_TTL_HOURS)

        return queueRequest
    }

    /**
     * 쿠폰 발급 요청을 Queue에 등록 (순수한 큐 관리만)
     *
     * @param userId 사용자 ID
     * @param coupon 쿠폰 정보
     * @return 등록된 Queue 요청
     * @throws CouponException.AlreadyInQueue 이미 Queue에 등록된 경우
     */
    fun enqueue(userId: Long, coupon: Coupon): CouponQueueRequest {
        val userMappingKey = getUserMappingKey(userId, coupon.id)

        // 중복 체크: 이미 Queue에 등록되어 있는지 확인
        val existingQueueId = RedisUtil.getValue(redisTemplate, userMappingKey)
        if (existingQueueId != null) {
            val existingRequest = getQueueRequest(existingQueueId)
            if (existingRequest != null && existingRequest.isActive()) {
                throw CouponException.AlreadyInQueue(userId, coupon.name)
            }
        }

        // Snowflake ID 생성
        val queueId = snowflakeGenerator.nextId().toString()
        val queueKey = getQueueKey(coupon.id)

        // 큐에 추가
        RedisUtil.enqueueItem(redisTemplate, queueKey, queueId)

        // Queue 순번 생성 (원자적 증가)
        val queuePosition = RedisUtil.incrementCounter(redisTemplate, getPositionCounterKey(coupon.id)).toInt()

        // Queue 요청 생성
        val queueRequest = CouponQueueRequest.create(
            queueId = queueId,
            userId = userId,
            couponId = coupon.id,
            couponName = coupon.name,
            queuePosition = queuePosition
        )

        // Queue 데이터 저장
        RedisUtil.saveJson(redisTemplate, objectMapper, getRequestKey(queueRequest.queueId), queueRequest, DEFAULT_TTL_HOURS)

        // User Mapping 저장 (중복 방지용)
        RedisUtil.setValue(redisTemplate, userMappingKey, queueRequest.queueId, DEFAULT_TTL_HOURS)

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
        val queueId = RedisUtil.dequeueItem(redisTemplate, queueKey)
            ?: return null

        // Request 데이터 조회
        val queueRequest = getQueueRequest(queueId)
            ?: return null

        // 상태를 PROCESSING으로 변경
        queueRequest.startProcessing()
        RedisUtil.saveJson(redisTemplate, objectMapper, getRequestKey(queueRequest.queueId), queueRequest, DEFAULT_TTL_HOURS)

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
        val jsonData = RedisUtil.getJson(redisTemplate, requestKey)
            ?: return null

        return objectMapper.readValue(jsonData, CouponQueueRequest::class.java)
    }


    /**
     * Queue 요청 상태 업데이트 (완료/실패 통합)
     *
     * @param queueId Queue ID
     * @param status 변경할 상태
     * @param result 결과값 (성공시 userCouponId, 실패시 reason)
     */
    fun updateQueueStatus(queueId: String, status: QueueStatus, result: Any? = null) {
        val queueRequest = getQueueRequest(queueId) ?: return

        when (status) {
            QueueStatus.COMPLETED -> {
                queueRequest.complete(result as Long)
            }
            QueueStatus.FAILED -> {
                queueRequest.fail(result as String)
            }
            else -> return
        }

        RedisUtil.saveJson(redisTemplate, objectMapper, getRequestKey(queueRequest.queueId), queueRequest, DEFAULT_TTL_HOURS)
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
        val queueId = RedisUtil.getValue(redisTemplate, userMappingKey)
            ?: return null

        return getQueueRequest(queueId)
    }


    // Redis Key 생성 메서드들
    private fun getQueueKey(couponId: Long): String = "$QUEUE_KEY_PREFIX:$couponId"
    private fun getRequestKey(queueId: String): String = "$REQUEST_KEY_PREFIX:$queueId"
    private fun getUserMappingKey(userId: Long, couponId: Long): String =
        "$USER_MAPPING_KEY_PREFIX:$userId:$couponId"
    private fun getPositionCounterKey(couponId: Long): String = "$POSITION_COUNTER_KEY_PREFIX:$couponId"
}
