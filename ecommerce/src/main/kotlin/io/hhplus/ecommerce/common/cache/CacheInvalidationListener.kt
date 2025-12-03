package io.hhplus.ecommerce.common.cache

import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

/**
 * 캐시 무효화 이벤트 리스너
 *
 * 역할:
 * - Redis Pub/Sub에서 캐시 무효화 이벤트 수신
 * - 로컬 캐시(Caffeine)에서 해당 엔트리 삭제
 *
 * 동작 방식:
 * 1. 다른 서버에서 CacheInvalidationPublisher로 이벤트 발행
 * 2. Redis Pub/Sub을 통해 모든 서버에 이벤트 전달
 * 3. 각 서버의 CacheInvalidationListener가 로컬 캐시 무효화
 */
@Component
class CacheInvalidationListener(
    private val localCacheManager: CacheManager
) : MessageListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 캐시 무효화 메시지 수신 시 호출
     *
     * @param message Redis 메시지 (형식: "cacheName:key" 또는 "cacheName:*")
     * @param pattern 구독 패턴 (사용하지 않음)
     */
    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val payload = String(message.body)
            logger.debug { "캐시 무효화 이벤트 수신: $payload" }

            val parts = payload.split(":", limit = 2)
            if (parts.size != 2) {
                logger.warn { "잘못된 캐시 무효화 메시지 형식: $payload" }
                return
            }

            val (cacheName, key) = parts
            invalidateLocalCache(cacheName, key)

        } catch (e: Exception) {
            logger.error(e) { "캐시 무효화 처리 중 오류 발생" }
        }
    }

    /**
     * 로컬 캐시에서 지정된 엔트리를 삭제한다
     */
    private fun invalidateLocalCache(cacheName: String, key: String) {
        val cache = localCacheManager.getCache(cacheName)

        if (cache == null) {
            logger.debug { "캐시를 찾을 수 없음: $cacheName" }
            return
        }

        if (key == "*") {
            // 전체 캐시 무효화
            cache.clear()
            logger.debug { "로컬 캐시 전체 무효화 완료: $cacheName" }
        } else {
            // 특정 키만 무효화
            cache.evict(key)
            logger.debug { "로컬 캐시 엔트리 무효화 완료: $cacheName:$key" }
        }
    }
}
