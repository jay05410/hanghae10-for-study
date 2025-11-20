package io.hhplus.ecommerce.product.application

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

/**
 * 상품 통계 Redis Cache 서비스
 *
 * 역할:
 * - 상품 조회수/판매량을 Redis에서 관리
 * - 고성능 카운터 연산 제공 (INCR/INCRBY)
 * - DB 동기화를 위한 캐시 데이터 조회
 */
@Service
class ProductStatisticsCacheService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    companion object {
        private const val VIEW_COUNT_PREFIX = "product:view:"
        private const val SALES_COUNT_PREFIX = "product:sales:"
    }

    /**
     * 조회수 증가 (Redis INCR)
     *
     * @param productId 상품 ID
     * @return 증가된 조회수
     */
    fun incrementViewCount(productId: Long): Long {
        val key = "$VIEW_COUNT_PREFIX$productId"
        return redisTemplate.opsForValue().increment(key) ?: 0L
    }

    /**
     * 판매량 증가 (Redis)
     *
     * @param productId 상품 ID
     * @param quantity 판매 수량
     * @return 증가된 판매량
     */
    fun incrementSalesCount(productId: Long, quantity: Int): Long {
        val key = "$SALES_COUNT_PREFIX$productId"
        return redisTemplate.opsForValue().increment(key, quantity.toLong()) ?: 0L
    }

    /**
     * 조회수 조회 (Redis)
     *
     * @param productId 상품 ID
     * @return Redis에 캐시된 조회수 (없으면 0)
     */
    fun getViewCount(productId: Long): Long {
        val key = "$VIEW_COUNT_PREFIX$productId"
        return redisTemplate.opsForValue().get(key)?.toString()?.toLong() ?: 0L
    }

    /**
     * 판매량 조회 (Redis)
     *
     * @param productId 상품 ID
     * @return Redis에 캐시된 판매량 (없으면 0)
     */
    fun getSalesCount(productId: Long): Long {
        val key = "$SALES_COUNT_PREFIX$productId"
        return redisTemplate.opsForValue().get(key)?.toString()?.toLong() ?: 0L
    }

    /**
     * Redis에 쌓인 조회수를 가져오고 삭제
     * (DB 동기화용)
     *
     * @param productId 상품 ID
     * @return 캐시된 조회수 (삭제 후 0으로 초기화)
     */
    fun getAndClearViewCount(productId: Long): Long {
        val key = "$VIEW_COUNT_PREFIX$productId"
        val value = redisTemplate.opsForValue().get(key)?.toString()?.toLong() ?: 0L
        if (value > 0) {
            redisTemplate.delete(key)
        }
        return value
    }

    /**
     * Redis에 쌓인 판매량을 가져오고 삭제
     * (DB 동기화용)
     *
     * @param productId 상품 ID
     * @return 캐시된 판매량 (삭제 후 0으로 초기화)
     */
    fun getAndClearSalesCount(productId: Long): Long {
        val key = "$SALES_COUNT_PREFIX$productId"
        val value = redisTemplate.opsForValue().get(key)?.toString()?.toLong() ?: 0L
        if (value > 0) {
            redisTemplate.delete(key)
        }
        return value
    }

    /**
     * 모든 상품의 조회수 키 목록 가져오기
     * (동기화 스케줄러용)
     *
     * @return 조회수 Redis 키 목록
     */
    fun getAllViewCountKeys(): Set<String> {
        return redisTemplate.keys("$VIEW_COUNT_PREFIX*") ?: emptySet()
    }

    /**
     * 모든 상품의 판매량 키 목록 가져오기
     * (동기화 스케줄러용)
     *
     * @return 판매량 Redis 키 목록
     */
    fun getAllSalesCountKeys(): Set<String> {
        return redisTemplate.keys("$SALES_COUNT_PREFIX*") ?: emptySet()
    }
}
