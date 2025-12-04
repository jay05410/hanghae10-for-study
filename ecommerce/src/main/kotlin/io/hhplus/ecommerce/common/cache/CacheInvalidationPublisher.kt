package io.hhplus.ecommerce.common.cache

import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 캐시 무효화 이벤트 발행자
 *
 * 역할:
 * - 로컬 캐시 변경 시 다른 서버에 무효화 이벤트 전파
 * - Redis Pub/Sub을 통한 분산 캐시 일관성 유지
 *
 * 사용 시나리오:
 * - 상품 정보 변경 시 → 모든 서버의 PRODUCT_DETAIL 캐시 무효화
 * - 쿠폰 정보 변경 시 → 모든 서버의 COUPON_INFO 캐시 무효화
 */
@Component
class CacheInvalidationPublisher(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        const val CACHE_INVALIDATION_CHANNEL = "cache:invalidation"
    }

    /**
     * 특정 캐시 키에 대한 무효화 이벤트를 발행한다
     *
     * @param cacheName 캐시 이름 (예: CacheNames.PRODUCT_DETAIL)
     * @param key 캐시 키 (예: productId)
     */
    fun publishInvalidation(cacheName: String, key: Any) {
        try {
            val message = "$cacheName:$key"
            redisTemplate.convertAndSend(CACHE_INVALIDATION_CHANNEL, message)
            logger.debug { "캐시 무효화 이벤트 발행: $message" }
        } catch (e: Exception) {
            logger.warn(e) { "캐시 무효화 이벤트 발행 실패: cacheName=$cacheName, key=$key" }
        }
    }

    /**
     * 특정 캐시의 모든 엔트리에 대한 무효화 이벤트를 발행한다
     *
     * @param cacheName 캐시 이름
     */
    fun publishInvalidateAll(cacheName: String) {
        try {
            val message = "$cacheName:*"
            redisTemplate.convertAndSend(CACHE_INVALIDATION_CHANNEL, message)
            logger.debug { "캐시 전체 무효화 이벤트 발행: $message" }
        } catch (e: Exception) {
            logger.warn(e) { "캐시 전체 무효화 이벤트 발행 실패: cacheName=$cacheName" }
        }
    }
}
