package io.hhplus.ecommerce.common.config

import io.hhplus.ecommerce.common.cache.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Spring Cache 설정
 */
@Configuration
@EnableCaching
class CacheConfig {

    /**
     * 캐시 매니저 (ConcurrentHashMap 기반)
     *
     * 장점:
     * - 추가 의존성 없음
     * - 간단한 설정
     * - 높은 성능 (JVM 힙 메모리)
     * - Micrometer 메트릭 자동 수집
     *
     * 단점:
     * - TTL 자동 만료 없음 (스케줄러로 관리)
     * - LRU 등 고급 기능 없음
     */
    @Bean
    @Primary
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()

        val caches = CacheNames.ALL_CACHE_NAMES.map { cacheName ->
            ConcurrentMapCache(cacheName)
        }

        cacheManager.setCaches(caches)
        return cacheManager
    }
}