package io.hhplus.ecommerce.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.ReturnType
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

    /**
     * 큐 크기 제한과 함께 원자적으로 아이템을 큐에 추가
     * Lua 스크립트를 사용하여 크기 체크와 LPUSH를 원자적으로 실행
     *
     * @param redisTemplate Redis 템플릿
     * @param queueKey 큐 키
     * @param item 추가할 아이템
     * @param maxSize 최대 허용 크기
     * @return true: 성공적으로 추가됨, false: 큐가 가득참
     */
    fun enqueueWithSizeCheck(
        redisTemplate: RedisTemplate<String, Any>,
        queueKey: String,
        item: String,
        maxSize: Int
    ): Boolean {
        val luaScript = """
            local queue_key = KEYS[1]
            local item = ARGV[1]
            local max_size = tonumber(ARGV[2])

            local current_size = redis.call('LLEN', queue_key)
            if current_size < max_size then
                redis.call('LPUSH', queue_key, item)
                return 1
            else
                return 0
            end
        """.trimIndent()

        val result = redisTemplate.execute<Long?> { connection ->
            connection.eval(
                luaScript.toByteArray(),
                ReturnType.INTEGER,
                1,
                queueKey.toByteArray(),
                item.toByteArray(),
                maxSize.toString().toByteArray()
            ) as? Long
        }

        return result == 1L
    }
}