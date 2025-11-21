package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.application.ProductStatisticsCacheService
import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ProductStatisticsService 단위 테스트
 *
 * Phase 4 최적화 후:
 * - Redis Cache 기반으로 조회수/판매량 증가
 * - DB는 조회 및 동기화 용도로만 사용
 * - 비관적 락 제거
 *
 * 검증 목표:
 * 1. incrementViewCount가 Redis에서 증가하는가?
 * 2. incrementSalesCount가 Redis에서 증가하는가?
 * 3. getProductStatistics가 Redis + DB를 합산하는가?
 * 4. 인기 상품 조회가 정상 동작하는가?
 */
class ProductStatisticsServiceTest : DescribeSpec({
    val mockProductStatisticsRepository = mockk<ProductStatisticsRepository>()
    val mockProductStatisticsCacheService = mockk<ProductStatisticsCacheService>()
    val sut = ProductStatisticsService(mockProductStatisticsRepository, mockProductStatisticsCacheService)

    beforeEach {
        clearMocks(mockProductStatisticsRepository, mockProductStatisticsCacheService)
    }

    describe("incrementViewCount") {
        context("Redis 캐시에서 조회수 증가") {
            it("ProductStatisticsCacheService의 incrementViewCount를 호출") {
                val productId = 1L
                val expectedCount = 1L

                every { mockProductStatisticsCacheService.incrementViewCount(productId) } returns expectedCount

                val result = sut.incrementViewCount(productId)

                result shouldBe expectedCount
                verify(exactly = 1) { mockProductStatisticsCacheService.incrementViewCount(productId) }
                verify(exactly = 0) { mockProductStatisticsRepository.findByProductIdWithLock(any()) }
            }
        }

    }

    describe("incrementSalesCount") {
        context("Redis 캐시에서 판매량 증가") {
            it("ProductStatisticsCacheService의 incrementSalesCount를 호출") {
                val productId = 1L
                val quantity = 5
                val expectedCount = 5L

                every { mockProductStatisticsCacheService.incrementSalesCount(productId, quantity) } returns expectedCount

                val result = sut.incrementSalesCount(productId, quantity)

                result shouldBe expectedCount
                verify(exactly = 1) { mockProductStatisticsCacheService.incrementSalesCount(productId, quantity) }
                verify(exactly = 0) { mockProductStatisticsRepository.findByProductIdWithLock(any()) }
            }
        }
    }

    describe("getPopularProducts") {
        context("인기 상품 목록 조회") {
            it("ProductStatisticsRepository에서 인기 상품을 조회하고 반환") {
                val limit = 10
                val mockStatistics = listOf(mockk<ProductStatistics>(), mockk<ProductStatistics>(), mockk<ProductStatistics>())

                every { mockProductStatisticsRepository.findTopPopularProducts(limit) } returns mockStatistics

                val result = sut.getPopularProducts(limit)

                result shouldBe mockStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findTopPopularProducts(limit) }
            }
        }

        context("다양한 limit으로 인기 상품 조회") {
            it("각 limit이 정확히 Repository에 전달") {
                val limits = listOf(5, 10, 20, 50)

                limits.forEach { limit ->
                    val mockStatistics = (1..limit).map { mockk<ProductStatistics>() }
                    every { mockProductStatisticsRepository.findTopPopularProducts(limit) } returns mockStatistics

                    val result = sut.getPopularProducts(limit)

                    result shouldBe mockStatistics
                    verify(exactly = 1) { mockProductStatisticsRepository.findTopPopularProducts(limit) }
                    clearMocks(mockProductStatisticsRepository)
                }
            }
        }

        context("인기 상품이 없는 경우") {
            it("빈 리스트를 반환") {
                val limit = 10

                every { mockProductStatisticsRepository.findTopPopularProducts(limit) } returns emptyList()

                val result = sut.getPopularProducts(limit)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductStatisticsRepository.findTopPopularProducts(limit) }
            }
        }
    }

    describe("getProductStatistics") {
        context("DB + Redis 합산하여 통계 조회") {
            it("DB 통계 + Redis 캐시를 합산하여 반환") {
                val productId = 1L
                val dbViewCount = 100L
                val dbSalesCount = 50L
                val redisViewCount = 10L
                val redisSalesCount = 5L

                val mockDbStats = ProductStatistics(
                    id = 1L,
                    productId = productId,
                    viewCount = dbViewCount,
                    salesCount = dbSalesCount
                )

                every { mockProductStatisticsRepository.findByProductId(productId) } returns mockDbStats
                every { mockProductStatisticsCacheService.getViewCount(productId) } returns redisViewCount
                every { mockProductStatisticsCacheService.getSalesCount(productId) } returns redisSalesCount

                val result = sut.getProductStatistics(productId)

                result!!.viewCount shouldBe (dbViewCount + redisViewCount)
                result.salesCount shouldBe (dbSalesCount + redisSalesCount)
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductId(productId) }
                verify(exactly = 1) { mockProductStatisticsCacheService.getViewCount(productId) }
                verify(exactly = 1) { mockProductStatisticsCacheService.getSalesCount(productId) }
            }
        }
    }
})