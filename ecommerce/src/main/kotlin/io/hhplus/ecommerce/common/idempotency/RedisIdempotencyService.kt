package io.hhplus.ecommerce.common.idempotency

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis 기반 멱등성 서비스 구현체
 *
 * 인프라 레이어에서 Redis를 사용하여 멱등성 보장
 * - Handler/Consumer는 이 구현체가 아닌 IdempotencyService 인터페이스에 의존
 * - DIP(의존성 역전 원칙) 준수
 */
@Component
class RedisIdempotencyService(
    private val redisTemplate: RedisTemplate<String, Any>
) : IdempotencyService {

    override fun isProcessed(key: String): Boolean {
        return redisTemplate.hasKey(key) == true
    }

    override fun markAsProcessed(key: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key, "1", ttl)
    }
}
