package io.hhplus.ecommerce.common.config

import io.hhplus.ecommerce.common.cache.CacheNames
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * 캐시 설정 - 로컬(Caffeine) + 분산(Redis) 하이브리드
 *
 * 캐시 전략:
 * - 로컬 캐시: 빠른 응답이 필요하고 서버별로 달라도 되는 데이터
 * - Redis 캐시: 서버간 일관성이 필요하고 분산 환경에서 공유되어야 하는 데이터
 *
 * TTL 전략:
 * - 상품 상세: 10분 (로컬 - 변경 빈도 낮음)
 * - 쿠폰 정보: 5분 (로컬 - 정기 업데이트)
 * - 상품 목록: 5분 (Redis - 서버간 일관성 필요)
 * - 카테고리별 목록: 5분 (Redis - 서버간 일관성 필요)
 * - 인기상품: 30초 (Redis - 실시간성 중요)
 */
@Configuration
@EnableCaching
class CacheConfig {

    /**
     * 로컬 캐시 매니저 (Caffeine 기반)
     * - 빠른 응답속도 (메모리 직접 접근)
     * - 자동 TTL 지원
     * - LRU 정책으로 메모리 관리
     * - 상품 상세, 쿠폰 정보 등에 사용
     */
    @Bean
    @Primary
    fun localCacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        // 기본 Caffeine 설정
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1000)  // 최대 1000개 항목
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 기본 10분 TTL
                .recordStats()  // 통계 수집 활성화
        )

        // 개별 캐시 설정 (필요시 캐시별 다른 설정 적용)
        cacheManager.setCacheSpecification(mapOf(
            CacheNames.PRODUCT_DETAIL to "maximumSize=500,expireAfterWrite=10m",
            CacheNames.COUPON_INFO to "maximumSize=200,expireAfterWrite=5m",
            CacheNames.COUPON_ACTIVE_LIST to "maximumSize=100,expireAfterWrite=5m"
        ))

        return cacheManager
    }

    /**
     * Redis 캐시 매니저
     * - 분산 환경에서 서버간 캐시 공유
     * - 네트워크 기반이지만 일관성 보장
     * - 상품 목록, 인기상품 등에 사용
     */
    @Bean("redisCacheManager")
    fun redisCacheManager(redisConnectionFactory: RedisConnectionFactory): RedisCacheManager {
        // 기본 Redis 캐시 설정
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))  // 기본 10분 TTL
            .disableCachingNullValues()  // null 값 캐싱 비활성화
            .serializeKeysWith(RedisCacheConfiguration.SerializationPair.fromSerializer(org.springframework.data.redis.serializer.StringRedisSerializer()))
            .serializeValuesWith(RedisCacheConfiguration.SerializationPair.fromSerializer(org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer()))

        // 캐시별 개별 TTL 설정
        val cacheConfigurations = mapOf(
            // 상품 목록: 5분 TTL (페이징 결과 일관성)
            CacheNames.PRODUCT_LIST to defaultConfig.entryTtl(Duration.ofMinutes(5)),

            // 카테고리별 상품 목록: 5분 TTL (카테고리별 결과 일관성)
            CacheNames.PRODUCT_CATEGORY_LIST to defaultConfig.entryTtl(Duration.ofMinutes(5)),

            // 인기상품 목록: 30초 TTL (실시간성 중요)
            CacheNames.PRODUCT_POPULAR to defaultConfig.entryTtl(Duration.ofSeconds(30))
        )

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()  // 트랜잭션 연동
            .build()
    }
}