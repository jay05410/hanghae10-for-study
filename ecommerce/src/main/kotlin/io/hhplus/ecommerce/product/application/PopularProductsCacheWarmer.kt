package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.product.usecase.GetProductQueryUseCase
import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 인기 상품 캐시 워밍 스케줄러
 *
 * 역할:
 * - 캐시 만료 전에 미리 갱신하여 Cache Stampede 방지
 * - 대량 트래픽 상황에서도 안정적인 응답 보장
 *
 * 전략:
 * - TTL: 1시간 (CacheConfig에서 설정)
 * - 갱신 주기: 30분마다 (TTL의 절반)
 * - 갱신 대상: limit 5, 10, 20 (일반적인 사용 패턴)
 *
 * 주의:
 * - 캐시 만료 시점에 동시 요청이 발생해도, 이미 캐시가 갱신되어 있음
 * - 서버 재시작 시에도 스케줄러가 자동으로 캐시를 채움
 */
@Component
class PopularProductsCacheWarmer(
    private val getProductQueryUseCase: GetProductQueryUseCase,
    private val cacheManager: CacheManager
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        // 일반적으로 사용되는 limit 값들
        private val WARM_UP_LIMITS = listOf(5, 10, 20)
    }

    /**
     * 30분마다 인기 상품 캐시를 갱신한다
     *
     * 실행 시점:
     * - 애플리케이션 시작 후 10초 뒤 첫 실행 (initialDelay)
     * - 이후 30분마다 반복 실행 (fixedRate)
     */
    @Scheduled(initialDelay = 10_000, fixedRate = 30 * 60 * 1000)
    fun warmPopularProductsCache() {
        logger.info { "인기 상품 캐시 워밍 시작" }

        try {
            WARM_UP_LIMITS.forEach { limit ->
                warmCacheForLimit(limit)
            }
            logger.info { "인기 상품 캐시 워밍 완료: limits=$WARM_UP_LIMITS" }
        } catch (e: Exception) {
            logger.error(e) { "인기 상품 캐시 워밍 실패" }
        }
    }

    /**
     * 특정 limit에 대한 캐시를 갱신한다
     */
    private fun warmCacheForLimit(limit: Int) {
        try {
            // 기존 캐시 삭제
            cacheManager.getCache(CacheNames.PRODUCT_POPULAR)?.evict(limit)

            // 새로운 데이터로 캐시 채우기 (@Cacheable이 자동으로 캐시에 저장)
            val products = getProductQueryUseCase.getPopularProducts(limit)

            logger.debug { "캐시 워밍 완료: limit=$limit, productCount=${products.size}" }
        } catch (e: Exception) {
            logger.warn(e) { "캐시 워밍 실패: limit=$limit" }
        }
    }
}
