package io.hhplus.ecommerce.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.TimeUnit

/**
 * Redis Queue 공통 유틸리티
 *
 * 여러 도메인에서 사용할 수 있는 Redis Queue 관련 공통 기능 제공
 */
object RedisUtil {

    /**
     * Queue에 아이템 추가 (LPUSH)
     */
    fun enqueueItem(redisTemplate: RedisTemplate<String, Any>, queueKey: String, item: String) {
        redisTemplate.opsForList().leftPush(queueKey, item)
    }

    /**
     * Queue에서 아이템 제거 (RPOP)
     */
    fun dequeueItem(redisTemplate: RedisTemplate<String, Any>, queueKey: String): String? {
        return redisTemplate.opsForList().rightPop(queueKey) as? String
    }

    /**
     * Queue 크기 조회
     */
    fun getQueueSize(redisTemplate: RedisTemplate<String, Any>, queueKey: String): Long {
        return redisTemplate.opsForList().size(queueKey) ?: 0L
    }

    /**
     * JSON 데이터 저장 (TTL 포함)
     */
    fun saveJson(
        redisTemplate: RedisTemplate<String, Any>,
        objectMapper: ObjectMapper,
        key: String,
        data: Any,
        ttlHours: Long
    ) {
        val jsonData = objectMapper.writeValueAsString(data)
        redisTemplate.opsForValue().set(key, jsonData, ttlHours, TimeUnit.HOURS)
    }

    /**
     * JSON 데이터 조회
     */
    fun getJson(redisTemplate: RedisTemplate<String, Any>, key: String): String? {
        return redisTemplate.opsForValue().get(key) as? String
    }

    /**
     * 카운터 원자적 증가
     */
    fun incrementCounter(redisTemplate: RedisTemplate<String, Any>, counterKey: String): Long {
        return redisTemplate.opsForValue().increment(counterKey)
            ?: throw IllegalStateException("카운터 증가 실패: $counterKey")
    }

    /**
     * 키-값 저장 (TTL 포함)
     */
    fun setValue(
        redisTemplate: RedisTemplate<String, Any>,
        key: String,
        value: String,
        ttlHours: Long
    ) {
        redisTemplate.opsForValue().set(key, value, ttlHours, TimeUnit.HOURS)
    }

    /**
     * 값 조회
     */
    fun getValue(redisTemplate: RedisTemplate<String, Any>, key: String): String? {
        return redisTemplate.opsForValue().get(key) as? String
    }
}