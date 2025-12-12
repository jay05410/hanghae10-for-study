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

    /**
     * 원자적으로 멱등성 키 획득 (SETNX with TTL)
     *
     * Redis SETNX는 키가 없을 때만 설정하는 원자적 연산
     * 경쟁 조건 없이 정확히 한 번만 처리 권한을 부여
     */
    override fun tryAcquire(key: String, ttl: Duration): Boolean {
        return redisTemplate.opsForValue().setIfAbsent(key, "1", ttl) == true
    }
}
