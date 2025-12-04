package io.hhplus.ecommerce.product.infra.ranking

import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.product.application.port.out.ProductRankingPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Redis 기반 상품 판매 랭킹 어댑터
 *
 * ProductRankingPort의 Redis 구현체.
 * Sorted Set을 활용하여 실시간 판매 랭킹을 관리.
 *
 * Redis 자료구조:
 * - Sorted Set (ZINCRBY, ZREVRANGE, ZRANK, ZSCORE)
 * - member: productId (String)
 * - score: 판매량 (Double)
 *
 * 키 구조:
 * - 일별: ecom:rank:sales:d:{yyyyMMdd}
 * - 주별: ecom:rank:sales:w:{yyyyWW}
 * - 누적: ecom:rank:sales:total
 *
 * 시간복잡도:
 * - ZINCRBY: O(log N) - 판매량 증가
 * - ZREVRANGE: O(log N + M) - Top N 조회
 * - ZSCORE: O(1) - 특정 상품 판매량 조회
 * - ZREVRANK: O(log N) - 특정 상품 순위 조회
 *
 * @see docs/WEEK07_RANKING_ASYNC_DESIGN_PLAN.md
 */
@Component
class RedisProductRankingAdapter(
    private val redisTemplate: RedisTemplate<String, Any>
) : ProductRankingPort {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val WEEK_FORMATTER = DateTimeFormatter.ofPattern("yyyyww", Locale.KOREA)
    }

    override fun incrementSalesCount(productId: Long, quantity: Int): Long {
        val today = LocalDate.now()
        val dateKey = today.format(DATE_FORMATTER)
        val weekKey = getYearWeek(today)

        val dailyKey = RedisKeyNames.Ranking.dailySalesKey(dateKey)
        val weeklyKey = RedisKeyNames.Ranking.weeklySalesKey(weekKey)
        val totalKey = RedisKeyNames.Ranking.totalSalesKey()

        val productIdStr = productId.toString()
        val incrementValue = quantity.toDouble()

        // 일별, 주별, 누적 랭킹 모두 원자적으로 업데이트 (ZINCRBY)
        val dailyScore = redisTemplate.opsForZSet()
            .incrementScore(dailyKey, productIdStr, incrementValue) ?: 0.0

        redisTemplate.opsForZSet()
            .incrementScore(weeklyKey, productIdStr, incrementValue)

        redisTemplate.opsForZSet()
            .incrementScore(totalKey, productIdStr, incrementValue)

        // TTL 설정 (일별: 7일, 주별: 30일)
        setDailyKeyExpire(dateKey, 7)
        setWeeklyKeyExpire(weekKey, 30)

        return dailyScore.toLong()
    }

    override fun getDailyTopProducts(date: String, limit: Int): List<Pair<Long, Long>> {
        val key = RedisKeyNames.Ranking.dailySalesKey(date)
        return getTopProductsFromZSet(key, limit)
    }

    override fun getWeeklyTopProducts(yearWeek: String, limit: Int): List<Pair<Long, Long>> {
        val key = RedisKeyNames.Ranking.weeklySalesKey(yearWeek)
        return getTopProductsFromZSet(key, limit)
    }

    override fun getTotalTopProducts(limit: Int): List<Pair<Long, Long>> {
        val key = RedisKeyNames.Ranking.totalSalesKey()
        return getTopProductsFromZSet(key, limit)
    }

    override fun getDailySalesCount(productId: Long, date: String): Long {
        val key = RedisKeyNames.Ranking.dailySalesKey(date)
        val score = redisTemplate.opsForZSet().score(key, productId.toString())
        return score?.toLong() ?: 0L
    }

    override fun getDailyRank(productId: Long, date: String): Long? {
        val key = RedisKeyNames.Ranking.dailySalesKey(date)
        return redisTemplate.opsForZSet().reverseRank(key, productId.toString())
    }

    override fun getTotalSalesCount(productId: Long): Long {
        val key = RedisKeyNames.Ranking.totalSalesKey()
        val score = redisTemplate.opsForZSet().score(key, productId.toString())
        return score?.toLong() ?: 0L
    }

    override fun getAllDailySales(date: String): Map<Long, Long> {
        val key = RedisKeyNames.Ranking.dailySalesKey(date)
        val result = mutableMapOf<Long, Long>()

        // 모든 멤버와 스코어 조회 (배치 동기화용)
        redisTemplate.opsForZSet()
            .rangeWithScores(key, 0, -1)
            ?.forEach { tuple ->
                val productId = tuple.value?.toString()?.toLongOrNull()
                val salesCount = tuple.score?.toLong()
                if (productId != null && salesCount != null) {
                    result[productId] = salesCount
                }
            }

        return result
    }

    override fun setDailyKeyExpire(date: String, ttlDays: Long) {
        val key = RedisKeyNames.Ranking.dailySalesKey(date)
        redisTemplate.expire(key, Duration.ofDays(ttlDays))
    }

    override fun setWeeklyKeyExpire(yearWeek: String, ttlDays: Long) {
        val key = RedisKeyNames.Ranking.weeklySalesKey(yearWeek)
        redisTemplate.expire(key, Duration.ofDays(ttlDays))
    }

    /**
     * Sorted Set에서 Top N 상품 조회 (공통 메서드)
     *
     * @param key Redis 키
     * @param limit 조회할 상품 수
     * @return 상품 ID와 판매량 쌍의 리스트
     */
    private fun getTopProductsFromZSet(key: String, limit: Int): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()

        // ZREVRANGE: 높은 점수(판매량) 순으로 조회
        redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, (limit - 1).toLong())
            ?.forEach { tuple ->
                val productId = tuple.value?.toString()?.toLongOrNull()
                val salesCount = tuple.score?.toLong()
                if (productId != null && salesCount != null) {
                    result.add(productId to salesCount)
                }
            }

        return result
    }

    /**
     * 현재 날짜의 연도-주차 문자열 반환
     *
     * @param date 날짜
     * @return yyyyWW 형식 (예: 202449)
     */
    private fun getYearWeek(date: LocalDate): String {
        val weekFields = WeekFields.of(Locale.KOREA)
        val year = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        return String.format("%d%02d", year, week)
    }
}
