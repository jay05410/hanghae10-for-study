package io.hhplus.ecommerce.unit.product.usecase

import io.hhplus.ecommerce.product.usecase.GetProductQueryUseCase
import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.product.application.EventBasedStatisticsService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.vo.ProductStatsVO
import io.hhplus.ecommerce.common.response.Cursor
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetProductQueryUseCase 단위 테스트
 *
 * 책임: 상품 조회 유스케이스의 비즈니스 로직 검증
 * - ProductQueryService와의 올바른 상호작용 확인
 * - EventBasedStatisticsService와의 연동 확인
 * - 커서 기반 페이징 로직 검증
 * - 예외 처리 검증
 *
 * 검증 목표:
 * 1. UseCase가 적절한 Service 메서드를 호출하는가?
 * 2. 파라미터를 올바르게 전달하는가?
 * 3. 결과를 올바르게 반환하는가?
 * 4. 예외가 Service에서 UseCase로 전파되는가?
 */
class GetProductQueryUseCaseTest : DescribeSpec({

    val mockProductQueryService = mockk<ProductQueryService>()
    val mockEventBasedStatisticsService = mockk<EventBasedStatisticsService>()
    val sut = GetProductQueryUseCase(mockProductQueryService, mockEventBasedStatisticsService)

    beforeEach {
        clearAllMocks()
    }

    describe("getProducts") {
        context("커서 기반 페이징으로 상품 목록을 조회할 때") {
            it("ProductQueryService의 getProductsWithCursor를 호출한다") {
                // given
                val lastId = 10L
                val size = 20
                val expectedCursor = mockk<Cursor<Product>>()
                every { mockProductQueryService.getProductsWithCursor(lastId, size) } returns expectedCursor

                // when
                val result = sut.getProducts(lastId, size)

                // then
                result shouldBe expectedCursor
                verify(exactly = 1) { mockProductQueryService.getProductsWithCursor(lastId, size) }
            }
        }
    }

    describe("getProduct") {
        context("상품 ID로 특정 상품을 조회할 때") {
            it("ProductQueryService의 getProduct를 호출한다") {
                // given
                val productId = 1L
                val expectedProduct = mockk<Product>()
                every { mockProductQueryService.getProduct(productId) } returns expectedProduct

                // when
                val result = sut.getProduct(productId)

                // then
                result shouldBe expectedProduct
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
            }
        }
    }

    describe("getProductsByCategory") {
        context("카테고리별 상품 목록을 조회할 때") {
            it("ProductQueryService의 getProductsByCategoryWithCursor를 호출한다") {
                // given
                val categoryId = 1L
                val lastId = 5L
                val size = 15
                val expectedCursor = mockk<Cursor<Product>>()
                every { mockProductQueryService.getProductsByCategoryWithCursor(categoryId, lastId, size) } returns expectedCursor

                // when
                val result = sut.getProductsByCategory(categoryId, lastId, size)

                // then
                result shouldBe expectedCursor
                verify(exactly = 1) { mockProductQueryService.getProductsByCategoryWithCursor(categoryId, lastId, size) }
            }
        }
    }

    describe("getProductStatistics") {
        context("상품 통계를 조회할 때") {
            it("EventBasedStatisticsService에서 통계를 가져와 ProductStatsVO를 생성한다") {
                // given
                val productId = 1L
                val viewCount = 100L
                val salesCount = 50L
                val wishCount = 25L

                every { mockEventBasedStatisticsService.getRealTimeStats(productId) } returns Triple(viewCount, salesCount, wishCount)

                // when
                val result = sut.getProductStatistics(productId)

                // then
                result.productId shouldBe productId
                result.viewCount shouldBe viewCount
                result.salesCount shouldBe salesCount
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimeStats(productId) }
            }
        }
    }

    describe("getPopularStatistics") {
        context("인기 상품 통계 목록을 조회할 때") {
            it("EventBasedStatisticsService에서 인기 상품을 가져와 ProductStatsVO 목록을 생성한다") {
                // given
                val limit = 10
                val popularProducts = listOf(
                    1L to 100L,
                    2L to 80L,
                    3L to 60L
                )

                every { mockEventBasedStatisticsService.getRealTimePopularProducts(limit) } returns popularProducts
                every { mockEventBasedStatisticsService.getRealTimeStats(1L) } returns Triple(100L, 50L, 25L)
                every { mockEventBasedStatisticsService.getRealTimeStats(2L) } returns Triple(80L, 40L, 20L)
                every { mockEventBasedStatisticsService.getRealTimeStats(3L) } returns Triple(60L, 30L, 15L)

                // when
                val result = sut.getPopularStatistics(limit)

                // then
                result.size shouldBe 3
                result[0].productId shouldBe 1L
                result[0].viewCount shouldBe 100L
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimePopularProducts(limit) }
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimeStats(1L) }
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimeStats(2L) }
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimeStats(3L) }
            }
        }
    }

    describe("getPopularProducts") {
        context("인기 상품 목록을 조회할 때 (캐시 적용)") {
            it("EventBasedStatisticsService와 ProductQueryService를 연동하여 상품 목록을 반환한다") {
                // given
                val limit = 5
                val popularProductIds = listOf(
                    1L to 100L,
                    2L to 80L
                )
                val product1 = mockk<Product>()
                val product2 = mockk<Product>()

                every { mockEventBasedStatisticsService.getRealTimePopularProducts(limit) } returns popularProductIds
                every { mockProductQueryService.getProduct(1L) } returns product1
                every { mockProductQueryService.getProduct(2L) } returns product2

                // when
                val result = sut.getPopularProducts(limit)

                // then
                result.size shouldBe 2
                result shouldBe listOf(product1, product2)
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimePopularProducts(limit) }
                verify(exactly = 1) { mockProductQueryService.getProduct(1L) }
                verify(exactly = 1) { mockProductQueryService.getProduct(2L) }
            }
        }

        context("기본 limit으로 인기 상품을 조회할 때") {
            it("기본값 10으로 조회한다") {
                // given
                val popularProductIds = listOf(1L to 100L)
                val product = mockk<Product>()

                every { mockEventBasedStatisticsService.getRealTimePopularProducts(10) } returns popularProductIds
                every { mockProductQueryService.getProduct(1L) } returns product

                // when
                val result = sut.getPopularProducts()

                // then
                result.size shouldBe 1
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimePopularProducts(10) }
            }
        }
    }
})