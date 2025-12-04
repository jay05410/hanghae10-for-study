package io.hhplus.ecommerce.coupon.infra.issue

import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.coupon.application.port.out.CouponIssuePort
import io.hhplus.ecommerce.coupon.application.port.out.CouponIssueResult
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis 기반 선착순 쿠폰 발급 어댑터
 *
 * CouponIssuePort의 Redis 구현체.
 * SADD + INCR + Soldout Flag 패턴으로 정확한 수량 제어.
 *
 * Redis 자료구조:
 * - SET (issued): 발급된 유저 목록 - SADD 원자성으로 중복 방지
 * - Counter: 발급 순번 - INCR 원자성으로 정확한 수량 제어
 * - Soldout Flag: 매진 상태 - 조기 종료로 불필요한 연산 방지
 * - ZSET (queue): 발급 대기열 - 순번 score로 선착순 보장
 *
 * 키 구조:
 * - 발급 SET: ecom:cpn:iss:issued:{couponId}
 * - 카운터: ecom:cpn:iss:counter:{couponId}
 * - 매진 플래그: ecom:cpn:iss:soldout:{couponId}
 * - 대기열 ZSET: ecom:cpn:iss:queue:{couponId}
 *
 * 시간복잡도:
 * - tryIssue: O(1) + O(1) + O(1) + O(log N) = O(log N)
 * - popFromQueue: O(log N * count)
 * - getIssuedCount/getQueueSize: O(1)
 */
@Component
class RedisCouponIssueAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : CouponIssuePort {

    override fun tryIssue(couponId: Long, userId: Long, maxQuantity: Int): CouponIssueResult {
        val issuedKey = RedisKeyNames.CouponIssue.issuedKey(couponId)
        val queueKey = RedisKeyNames.CouponIssue.queueKey(couponId)
        val counterKey = RedisKeyNames.CouponIssue.counterKey(couponId)
        val soldoutKey = RedisKeyNames.CouponIssue.soldoutKey(couponId)
        val userIdStr = userId.toString()

        // 1. 매진 플래그 조기 체크 (O(1), 빠른 실패)
        if (redisTemplate.hasKey(soldoutKey) == true) {
            return CouponIssueResult.SOLD_OUT
        }

        // 2. SADD로 원자적 중복 체크 + 등록
        val added = redisTemplate.opsForSet().add(issuedKey, userIdStr)
        if (added == null || added == 0L) {
            return CouponIssueResult.ALREADY_ISSUED
        }

        // 3. INCR로 원자적 순번 획득 - "내가 몇 번째인가"를 정확히 알 수 있음
        val myOrder = redisTemplate.opsForValue().increment(counterKey) ?: 1L

        // 4. 순번이 maxQuantity보다 크면 롤백 + 매진 플래그 설정
        if (myOrder > maxQuantity) {
            redisTemplate.opsForSet().remove(issuedKey, userIdStr)
            // 매진 플래그 설정 (이후 요청은 1번에서 바로 실패)
            redisTemplate.opsForValue().set(soldoutKey, "1")
            return CouponIssueResult.SOLD_OUT
        }

        // 5. 성공 시 ZADD로 대기열 등록 (순번을 score로 사용하여 선착순 보장)
        redisTemplate.opsForZSet().add(queueKey, userIdStr, myOrder.toDouble())

        return CouponIssueResult.QUEUED
    }

    override fun popFromQueue(couponId: Long, count: Int): List<Long> {
        val queueKey = RedisKeyNames.CouponIssue.queueKey(couponId)

        // ZPOPMIN으로 가장 오래된(낮은 score) 항목부터 꺼냄
        val result = mutableListOf<Long>()
        repeat(count) {
            val popped = redisTemplate.opsForZSet().popMin(queueKey)
            if (popped != null) {
                val userId = popped.value?.toString()?.toLongOrNull()
                if (userId != null) {
                    result.add(userId)
                }
            }
        }
        return result
    }

    override fun getIssuedCount(couponId: Long): Long {
        val issuedKey = RedisKeyNames.CouponIssue.issuedKey(couponId)
        return redisTemplate.opsForSet().size(issuedKey) ?: 0L
    }

    override fun getQueueSize(couponId: Long): Long {
        val queueKey = RedisKeyNames.CouponIssue.queueKey(couponId)
        return redisTemplate.opsForZSet().size(queueKey) ?: 0L
    }

    override fun isAlreadyIssued(couponId: Long, userId: Long): Boolean {
        val issuedKey = RedisKeyNames.CouponIssue.issuedKey(couponId)
        return redisTemplate.opsForSet().isMember(issuedKey, userId.toString()) ?: false
    }

    override fun clearIssueData(couponId: Long) {
        val issuedKey = RedisKeyNames.CouponIssue.issuedKey(couponId)
        val queueKey = RedisKeyNames.CouponIssue.queueKey(couponId)
        val counterKey = RedisKeyNames.CouponIssue.counterKey(couponId)
        val soldoutKey = RedisKeyNames.CouponIssue.soldoutKey(couponId)
        val maxQtyKey = RedisKeyNames.CouponIssue.maxQuantityKey(couponId)
        redisTemplate.delete(listOf(issuedKey, queueKey, counterKey, soldoutKey, maxQtyKey))
    }

    override fun setMaxQuantity(couponId: Long, maxQuantity: Int) {
        val maxQtyKey = RedisKeyNames.CouponIssue.maxQuantityKey(couponId)
        redisTemplate.opsForValue().set(maxQtyKey, maxQuantity)
    }

    override fun getMaxQuantity(couponId: Long): Int? {
        val maxQtyKey = RedisKeyNames.CouponIssue.maxQuantityKey(couponId)
        return (redisTemplate.opsForValue().get(maxQtyKey) as? Number)?.toInt()
    }

    override fun tryIssueWithStoredQuantity(couponId: Long, userId: Long): CouponIssueResult {
        val maxQuantity = getMaxQuantity(couponId)
            ?: return CouponIssueResult.SOLD_OUT // maxQuantity가 없으면 발급 불가

        return tryIssue(couponId, userId, maxQuantity)
    }
}