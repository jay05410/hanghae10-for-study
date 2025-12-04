package io.hhplus.ecommerce.product.application.usecase

import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.product.application.port.out.ProductRankingPort
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.service.ProductDomainService
import io.hhplus.ecommerce.product.presentation.dto.response.ProductRankingResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 상품 판매 랭킹 조회 UseCase
 *
 * 역할:
 * - Redis Sorted Set 기반 판매 랭킹 조회
 * - 일별/주별/누적 랭킹 제공
 * - 특정 상품의 순위 조회
 *
 * Redis 자료구조:
 * - ZREVRANGE: Top N 상품 조회 (O(log N + M))
 * - ZREVRANK: 특정 상품 순위 조회 (O(log N))
 * - ZSCORE: 특정 상품 판매량 조회 (O(1))
 *
 * @see docs/WEEK07_RANKING_ASYNC_DESIGN_PLAN.md
 */
@Component
class ProductRankingQueryUseCase(
    private val productRankingPort: ProductRankingPort,
    private val productDomainService: ProductDomainService
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    /**
     * 오늘의 판매 랭킹 Top N 상품 조회
     *
     * @param limit 조회할 상품 수 (기본 10개)
     * @return 일별 판매 랭킹 응답 목록
     */
    @Cacheable(value = [CacheNames.PRODUCT_RANKING], key = "'daily:today:' + #limit", cacheManager = "redisCacheManager")
    fun getTodayTopProducts(limit: Int = 10): List<ProductRankingResponse> {
        val today = LocalDate.now().format(DATE_FORMATTER)
        return getDailyTopProducts(today, limit)
    }

    /**
     * 특정 날짜의 판매 랭킹 Top N 상품 조회
     *
     * @param date 조회할 날짜 (yyyyMMdd 형식)
     * @param limit 조회할 상품 수
     * @return 일별 판매 랭킹 응답 목록
     */
    fun getDailyTopProducts(date: String, limit: Int = 10): List<ProductRankingResponse> {
        val topProducts = productRankingPort.getDailyTopProducts(date, limit)
        return toRankingResponses(topProducts)
    }

    /**
     * 이번 주 판매 랭킹 Top N 상품 조회
     *
     * @param limit 조회할 상품 수 (기본 10개)
     * @return 주간 판매 랭킹 응답 목록
     */
    @Cacheable(value = [CacheNames.PRODUCT_RANKING], key = "'weekly:this:' + #limit", cacheManager = "redisCacheManager")
    fun getThisWeekTopProducts(limit: Int = 10): List<ProductRankingResponse> {
        val thisWeek = getYearWeek(LocalDate.now())
        return getWeeklyTopProducts(thisWeek, limit)
    }

    /**
     * 특정 주차의 판매 랭킹 Top N 상품 조회
     *
     * @param yearWeek 조회할 주차 (yyyyWW 형식)
     * @param limit 조회할 상품 수
     * @return 주간 판매 랭킹 응답 목록
     */
    fun getWeeklyTopProducts(yearWeek: String, limit: Int = 10): List<ProductRankingResponse> {
        val topProducts = productRankingPort.getWeeklyTopProducts(yearWeek, limit)
        return toRankingResponses(topProducts)
    }

    /**
     * 누적 판매 랭킹 Top N 상품 조회
     *
     * @param limit 조회할 상품 수 (기본 10개)
     * @return 누적 판매 랭킹 응답 목록
     */
    @Cacheable(value = [CacheNames.PRODUCT_RANKING], key = "'total:' + #limit", cacheManager = "redisCacheManager")
    fun getTotalTopProducts(limit: Int = 10): List<ProductRankingResponse> {
        val topProducts = productRankingPort.getTotalTopProducts(limit)
        return toRankingResponses(topProducts)
    }

    /**
     * 특정 상품의 오늘 판매 순위 및 판매량 조회
     *
     * @param productId 상품 ID
     * @return 랭킹 응답 (순위 포함)
     */
    fun getProductTodayRanking(productId: Long): ProductRankingResponse {
        val today = LocalDate.now().format(DATE_FORMATTER)
        val rank = productRankingPort.getDailyRank(productId, today)
        val salesCount = productRankingPort.getDailySalesCount(productId, today)
        val product = productDomainService.getProduct(productId)

        return ProductRankingResponse(
            rank = rank?.plus(1)?.toInt() ?: 0, // 0-based → 1-based
            productId = product.id,
            productName = product.name,
            salesCount = salesCount
        )
    }

    /**
     * 특정 상품의 누적 판매 순위 및 판매량 조회
     *
     * @param productId 상품 ID
     * @return 랭킹 응답 (순위 포함)
     */
    fun getProductTotalRanking(productId: Long): ProductRankingResponse {
        val salesCount = productRankingPort.getTotalSalesCount(productId)
        val product = productDomainService.getProduct(productId)

        // 누적 랭킹에서 순위 조회 (별도 메서드 필요 시 추가)
        return ProductRankingResponse(
            rank = 0, // 순위는 별도 조회 필요
            productId = product.id,
            productName = product.name,
            salesCount = salesCount
        )
    }

    /**
     * 랭킹 데이터를 응답 DTO로 변환
     */
    private fun toRankingResponses(topProducts: List<Pair<Long, Long>>): List<ProductRankingResponse> {
        return topProducts.mapIndexed { index, (productId, salesCount) ->
            val product = try {
                productDomainService.getProduct(productId)
            } catch (e: Exception) {
                null
            }

            ProductRankingResponse(
                rank = index + 1,
                productId = productId,
                productName = product?.name ?: "Unknown",
                salesCount = salesCount
            )
        }
    }

    /**
     * 현재 날짜의 연도-주차 문자열 반환
     */
    private fun getYearWeek(date: LocalDate): String {
        val weekFields = WeekFields.of(Locale.KOREA)
        val year = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        return String.format("%d%02d", year, week)
    }
}
