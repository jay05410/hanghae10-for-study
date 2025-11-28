package io.hhplus.ecommerce.unit.product.usecase

import io.hhplus.ecommerce.product.usecase.GetProductQueryUseCase
import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.application.EventBasedStatisticsService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.common.response.Cursor
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetProductQueryUseCase 단위 테스트
 *
 * 책임: 상품 조회 유스케이스의 비즈니스 로직 검증
 * - ProductService와의 올바른 상호작용 확인
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
    val mockProductService = mockk<ProductService>()
    val mockEventBasedStatisticsService = mockk<EventBasedStatisticsService>()
    val sut = GetProductQueryUseCase(mockProductService, mockEventBasedStatisticsService)

    beforeEach {
        clearMocks(mockProductService, mockEventBasedStatisticsService)
    }

    describe("getProducts") {
        context("커서 기반 상품 목록 조회") {
            it("ProductService의 getProductsWithCursor를 호출하고 결과를 반환") {
                val lastId = 10L
                val size = 20
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())
                val mockCursor = Cursor.from(mockProducts, 20L)

                every { mockProductService.getProductsWithCursor(lastId, size) } returns mockCursor

                val result = sut.getProducts(lastId, size)

                result shouldBe mockCursor
                verify(exactly = 1) { mockProductService.getProductsWithCursor(lastId, size) }
            }
        }

        context("첫 페이지 조회 (lastId가 null)") {
            it("ProductService의 getProductsWithCursor를 null과 함께 호출") {
                val lastId = null
                val size = 20
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())
                val mockCursor = Cursor.from(mockProducts, null)

                every { mockProductService.getProductsWithCursor(lastId, size) } returns mockCursor

                val result = sut.getProducts(lastId, size)

                result shouldBe mockCursor
                verify(exactly = 1) { mockProductService.getProductsWithCursor(null, size) }
            }
        }
    }

    describe("getProduct") {
        context("특정 상품 조회") {
            it("ProductService의 getProduct를 호출하고 결과를 반환") {
                val productId = 1L
                val mockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct

                val result = sut.getProduct(productId)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductService.getProduct(productId) }
            }
        }
    }

    describe("getProductsByCategory") {
        context("카테고리별 상품 목록 조회 (커서 기반)") {
            it("ProductService의 getProductsByCategoryWithCursor를 호출하고 결과를 반환") {
                val categoryId = 1L
                val lastId = 5L
                val size = 15
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())
                val mockCursor = Cursor.from(mockProducts, 10L)

                every { mockProductService.getProductsByCategoryWithCursor(categoryId, lastId, size) } returns mockCursor

                val result = sut.getProductsByCategory(categoryId, lastId, size)

                result shouldBe mockCursor
                verify(exactly = 1) { mockProductService.getProductsByCategoryWithCursor(categoryId, lastId, size) }
            }
        }

        context("첫 페이지 카테고리 조회") {
            it("ProductService의 getProductsByCategoryWithCursor를 null과 함께 호출") {
                val categoryId = 1L
                val lastId = null
                val size = 15
                val mockProducts = listOf(mockk<Product>())
                val mockCursor = Cursor.from(mockProducts, null)

                every { mockProductService.getProductsByCategoryWithCursor(categoryId, lastId, size) } returns mockCursor

                val result = sut.getProductsByCategory(categoryId, lastId, size)

                result shouldBe mockCursor
                verify(exactly = 1) { mockProductService.getProductsByCategoryWithCursor(categoryId, null, size) }
            }
        }
    }

    describe("getPopularProducts") {
        context("인기 상품 목록 조회") {
            it("EventBasedStatisticsService에서 인기 상품 ID를 조회하고 상품 정보를 조회") {
                val limit = 10
                val popularStats = listOf(1L to 100L, 2L to 80L, 3L to 60L)
                val mockProduct1 = mockk<Product>()
                val mockProduct2 = mockk<Product>()
                val mockProduct3 = mockk<Product>()

                every { mockEventBasedStatisticsService.getRealTimePopularProducts(limit) } returns popularStats
                every { mockProductService.getProduct(1L) } returns mockProduct1
                every { mockProductService.getProduct(2L) } returns mockProduct2
                every { mockProductService.getProduct(3L) } returns mockProduct3

                val result = sut.getPopularProducts(limit)

                result shouldBe listOf(mockProduct1, mockProduct2, mockProduct3)
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimePopularProducts(limit) }
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
                verify(exactly = 1) { mockProductService.getProduct(3L) }
            }
        }

        context("기본 limit으로 인기 상품 조회") {
            it("기본값 10으로 인기 상품을 조회") {
                val popularStats = listOf(1L to 100L, 2L to 80L)
                val mockProduct1 = mockk<Product>()
                val mockProduct2 = mockk<Product>()

                every { mockEventBasedStatisticsService.getRealTimePopularProducts(10) } returns popularStats
                every { mockProductService.getProduct(1L) } returns mockProduct1
                every { mockProductService.getProduct(2L) } returns mockProduct2

                val result = sut.getPopularProducts()

                result shouldBe listOf(mockProduct1, mockProduct2)
                verify(exactly = 1) { mockEventBasedStatisticsService.getRealTimePopularProducts(10) }
            }
        }
    }
})