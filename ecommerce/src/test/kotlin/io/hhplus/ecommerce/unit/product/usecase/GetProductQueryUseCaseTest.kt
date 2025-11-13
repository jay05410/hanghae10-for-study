package io.hhplus.ecommerce.unit.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.usecase.GetProductQueryUseCase
import io.hhplus.ecommerce.product.domain.entity.Product
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetProductQueryUseCase 단위 테스트
 *
 * 책임: 상품 조회 요청을 ProductService로 위임하는 역할 검증
 * - 상품 목록 조회 기능의 ProductService 호출 검증
 * - 단일 상품 조회 기능의 ProductService 호출 검증
 * - 카테고리별 상품 조회 기능의 ProductService 호출 검증
 * - 파라미터 전달 및 결과 반환의 정확성 검증
 *
 * 검증 목표:
 * 1. getProducts 메서드가 올바른 page로 ProductService를 호출하는가?
 * 2. getProduct 메서드가 올바른 productId로 ProductService를 호출하는가?
 * 3. getProductsByCategory 메서드가 올바른 categoryId로 ProductService를 호출하는가?
 * 4. ProductService의 결과가 그대로 반환되는가?
 * 5. 다양한 입력값에 대한 정확한 처리가 이루어지는가?
 */
class GetProductQueryUseCaseTest : DescribeSpec({
    val mockProductService = mockk<ProductService>()
    val sut = GetProductQueryUseCase(mockProductService)

    beforeEach {
        clearMocks(mockProductService)
    }

    describe("getProducts") {
        context("페이지 번호로 상품 목록 조회") {
            it("ProductService.getProducts를 호출하고 결과를 반환") {
                val page = 1
                val mockProducts = listOf(mockk<Product>(), mockk<Product>(), mockk<Product>())

                every { mockProductService.getProducts(page) } returns mockProducts

                val result = sut.getProducts(page)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductService.getProducts(page) }
            }
        }

        context("다양한 페이지 번호로 조회") {
            it("각 page가 정확히 ProductService에 전달") {
                val pages = listOf(1, 2, 5, 10, 50)

                pages.forEach { page ->
                    val mockProducts = listOf(mockk<Product>())
                    every { mockProductService.getProducts(page) } returns mockProducts

                    val result = sut.getProducts(page)

                    result shouldBe mockProducts
                    verify(exactly = 1) { mockProductService.getProducts(page) }
                    clearMocks(mockProductService)
                }
            }
        }

        context("상품이 없는 페이지 조회") {
            it("ProductService.getProducts를 호출하고 빈 리스트를 반환") {
                val page = 999

                every { mockProductService.getProducts(page) } returns emptyList()

                val result = sut.getProducts(page)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductService.getProducts(page) }
            }
        }

        context("경계값 테스트") {
            it("최소값과 큰 값의 page로 조회") {
                val boundaryPages = listOf(1, Int.MAX_VALUE)

                boundaryPages.forEach { page ->
                    val mockProducts = listOf(mockk<Product>())
                    every { mockProductService.getProducts(page) } returns mockProducts

                    val result = sut.getProducts(page)

                    result shouldBe mockProducts
                    verify(exactly = 1) { mockProductService.getProducts(page) }
                    clearMocks(mockProductService)
                }
            }
        }
    }

    describe("getProduct") {
        context("존재하는 상품 ID로 조회") {
            it("ProductService.getProduct를 호출하고 결과를 반환") {
                val productId = 1L
                val mockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct

                val result = sut.getProduct(productId)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductService.getProduct(productId) }
            }
        }

        context("다양한 상품 ID로 조회") {
            it("각 productId가 정확히 ProductService에 전달") {
                val productIds = listOf(1L, 100L, 999L, Long.MAX_VALUE)

                productIds.forEach { productId ->
                    val mockProduct = mockk<Product>()
                    every { mockProductService.getProduct(productId) } returns mockProduct

                    val result = sut.getProduct(productId)

                    result shouldBe mockProduct
                    verify(exactly = 1) { mockProductService.getProduct(productId) }
                    clearMocks(mockProductService)
                }
            }
        }

        context("경계값 테스트") {
            it("최소값과 최대값 productId로 조회") {
                val boundaryProductIds = listOf(1L, Long.MAX_VALUE)

                boundaryProductIds.forEach { productId ->
                    val mockProduct = mockk<Product>()
                    every { mockProductService.getProduct(productId) } returns mockProduct

                    val result = sut.getProduct(productId)

                    result shouldBe mockProduct
                    verify(exactly = 1) { mockProductService.getProduct(productId) }
                    clearMocks(mockProductService)
                }
            }
        }
    }

    describe("getProductsByCategory") {
        context("존재하는 카테고리 ID로 조회") {
            it("ProductService.getProductsByCategory를 호출하고 결과를 반환") {
                val categoryId = 1L
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())

                every { mockProductService.getProductsByCategory(categoryId) } returns mockProducts

                val result = sut.getProductsByCategory(categoryId)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductService.getProductsByCategory(categoryId) }
            }
        }

        context("상품이 없는 카테고리로 조회") {
            it("ProductService.getProductsByCategory를 호출하고 빈 리스트를 반환") {
                val categoryId = 999L

                every { mockProductService.getProductsByCategory(categoryId) } returns emptyList()

                val result = sut.getProductsByCategory(categoryId)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductService.getProductsByCategory(categoryId) }
            }
        }

        context("다양한 카테고리 ID로 조회") {
            it("각 categoryId가 정확히 ProductService에 전달") {
                val categoryIds = listOf(1L, 5L, 10L, 99L, Long.MAX_VALUE)

                categoryIds.forEach { categoryId ->
                    val mockProducts = listOf(mockk<Product>())
                    every { mockProductService.getProductsByCategory(categoryId) } returns mockProducts

                    val result = sut.getProductsByCategory(categoryId)

                    result shouldBe mockProducts
                    verify(exactly = 1) { mockProductService.getProductsByCategory(categoryId) }
                    clearMocks(mockProductService)
                }
            }
        }

        context("대량의 상품이 있는 카테고리") {
            it("ProductService.getProductsByCategory를 호출하고 모든 상품을 반환") {
                val categoryId = 1L
                val mockProducts = (1..100).map { mockk<Product>() }

                every { mockProductService.getProductsByCategory(categoryId) } returns mockProducts

                val result = sut.getProductsByCategory(categoryId)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductService.getProductsByCategory(categoryId) }
            }
        }
    }

    describe("메서드별 독립성 검증") {
        context("각 메서드 호출 시 다른 메서드 호출되지 않음") {
            it("getProducts 호출 시 다른 ProductService 메서드가 호출되지 않음") {
                val page = 1
                val mockProducts = listOf(mockk<Product>())

                every { mockProductService.getProducts(page) } returns mockProducts
                every { mockProductService.getProduct(any()) } returns mockk()
                every { mockProductService.getProductsByCategory(any()) } returns mockProducts

                sut.getProducts(page)

                verify(exactly = 1) { mockProductService.getProducts(page) }
                verify(exactly = 0) { mockProductService.getProduct(any()) }
                verify(exactly = 0) { mockProductService.getProductsByCategory(any()) }
            }
        }

        context("getProduct 호출 시 다른 메서드 호출되지 않음") {
            it("getProduct만 호출하고 다른 ProductService 메서드는 호출하지 않음") {
                val productId = 1L
                val mockProduct = mockk<Product>()
                val mockProducts = listOf(mockProduct)

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.getProducts(any()) } returns mockProducts
                every { mockProductService.getProductsByCategory(any()) } returns mockProducts

                sut.getProduct(productId)

                verify(exactly = 1) { mockProductService.getProduct(productId) }
                verify(exactly = 0) { mockProductService.getProducts(any()) }
                verify(exactly = 0) { mockProductService.getProductsByCategory(any()) }
            }
        }

        context("getProductsByCategory 호출 시 다른 메서드 호출되지 않음") {
            it("getProductsByCategory만 호출하고 다른 ProductService 메서드는 호출하지 않음") {
                val categoryId = 1L
                val mockProducts = listOf(mockk<Product>())

                every { mockProductService.getProductsByCategory(categoryId) } returns mockProducts
                every { mockProductService.getProducts(any()) } returns mockProducts
                every { mockProductService.getProduct(any()) } returns mockk()

                sut.getProductsByCategory(categoryId)

                verify(exactly = 1) { mockProductService.getProductsByCategory(categoryId) }
                verify(exactly = 0) { mockProductService.getProducts(any()) }
                verify(exactly = 0) { mockProductService.getProduct(any()) }
            }
        }
    }

    describe("위임 패턴 검증") {
        context("UseCase의 역할") {
            it("비즈니스 로직 없이 ProductService로 단순 위임") {
                val page = 1
                val mockProducts = listOf(mockk<Product>())

                every { mockProductService.getProducts(page) } returns mockProducts

                val result = sut.getProducts(page)

                // 결과가 ProductService에서 온 것과 동일한지 확인
                result shouldBe mockProducts

                // ProductService가 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockProductService.getProducts(page) }
            }
        }

        context("결과 변환 없음 확인") {
            it("ProductService 결과를 변환 없이 그대로 반환") {
                val productId = 1L
                val mockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct

                val result = sut.getProduct(productId)

                // 참조가 동일한지 확인 (새로운 객체를 만들지 않았는지)
                result shouldBe mockProduct
                verify(exactly = 1) { mockProductService.getProduct(productId) }
            }
        }

        context("카테고리 조회 결과 변환 없음 확인") {
            it("ProductService의 카테고리별 조회 결과를 변환 없이 그대로 반환") {
                val categoryId = 1L
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())

                every { mockProductService.getProductsByCategory(categoryId) } returns mockProducts

                val result = sut.getProductsByCategory(categoryId)

                // 참조가 동일한지 확인 (새로운 객체를 만들지 않았는지)
                result shouldBe mockProducts
                verify(exactly = 1) { mockProductService.getProductsByCategory(categoryId) }
            }
        }
    }
})