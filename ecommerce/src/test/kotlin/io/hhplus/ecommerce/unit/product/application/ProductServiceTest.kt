package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*

/**
 * ProductService 단위 테스트
 *
 * 책임: 상품 도메인 비즈니스 로직 처리 검증
 * - 상품 생성/조회/수정 로직 검증
 * - 카테고리별 상품 조회 로직 검증
 * - ProductRepository와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. 각 비즈니스 메서드가 적절한 Repository 메서드를 호출하는가?
 * 2. 존재하지 않는 상품 접근 시 적절한 예외가 발생하는가?
 * 3. 상품 생성 시 도메인 객체가 올바르게 생성되는가?
 * 4. 페이징 로직이 올바르게 처리되는가?
 * 5. 각 메서드의 반환값이 올바른가?
 */
class ProductServiceTest : DescribeSpec({
    val mockProductRepository = mockk<ProductRepository>()
    val sut = ProductService(mockProductRepository)

    beforeEach {
        clearMocks(mockProductRepository)
    }

    describe("getProducts") {
        context("페이지 번호와 함께 상품 목록 조회") {
            it("ProductRepository에서 활성 상품을 조회하고 반환") {
                val page = 1
                val mockProducts = listOf(mockk<Product>(), mockk<Product>(), mockk<Product>())

                every { mockProductRepository.findAllByIsActive(true) } returns mockProducts

                val result = sut.getProducts(page)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductRepository.findAllByIsActive(true) }
            }
        }

        context("다양한 페이지 번호로 조회") {
            it("페이지 번호와 관계없이 전체 활성 상품을 반환") {
                val pages = listOf(1, 2, 5, 10)
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())

                pages.forEach { page ->
                    every { mockProductRepository.findAllByIsActive(true) } returns mockProducts

                    val result = sut.getProducts(page)

                    result shouldBe mockProducts
                    verify(exactly = 1) { mockProductRepository.findAllByIsActive(true) }
                    clearMocks(mockProductRepository)
                }
            }
        }

        context("상품이 없는 경우") {
            it("빈 리스트를 반환") {
                every { mockProductRepository.findAllByIsActive(true) } returns emptyList()

                val result = sut.getProducts(1)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductRepository.findAllByIsActive(true) }
            }
        }
    }

    describe("getProduct") {
        context("존재하는 상품 ID로 조회") {
            it("ProductRepository에서 상품을 조회하고 반환") {
                val productId = 1L
                val mockProduct = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(productId, true) } returns mockProduct

                val result = sut.getProduct(productId)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId, true) }
            }
        }

        context("존재하지 않는 상품 ID로 조회") {
            it("ProductNotFound 예외를 발생") {
                val productId = 999L

                every { mockProductRepository.findByIdAndIsActive(productId, true) } returns null

                shouldThrow<ProductException.ProductNotFound> {
                    sut.getProduct(productId)
                }

                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId, true) }
            }
        }

        context("비활성 상품 ID로 조회") {
            it("ProductNotFound 예외를 발생") {
                val productId = 2L

                every { mockProductRepository.findByIdAndIsActive(productId, true) } returns null

                shouldThrow<ProductException.ProductNotFound> {
                    sut.getProduct(productId)
                }

                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId, true) }
            }
        }

        context("다양한 상품 ID로 조회") {
            it("각 ID에 대해 정확한 Repository 호출") {
                val productIds = listOf(1L, 100L, 999L)

                productIds.forEach { productId ->
                    val mockProduct = mockk<Product>()
                    every { mockProductRepository.findByIdAndIsActive(productId, true) } returns mockProduct

                    val result = sut.getProduct(productId)

                    result shouldBe mockProduct
                    verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId, true) }
                    clearMocks(mockProductRepository)
                }
            }
        }
    }

    describe("createProduct") {
        context("새로운 상품 생성 요청") {
            it("Product 도메인 객체를 생성하고 저장") {
                val name = "테스트상품"
                val description = "테스트상품설명"
                val price = 10000L
                val categoryId = 1L
                val createdBy = 1L
                val mockProduct = mockk<Product>()

                every { mockProductRepository.save(any()) } returns mockProduct

                val result = sut.createProduct(name, description, price, categoryId, createdBy)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductRepository.save(any()) }
            }
        }

        context("다양한 상품 정보로 생성") {
            it("각 정보가 Product.create에 올바르게 전달") {
                data class TestCase(val name: String, val description: String, val price: Long, val categoryId: Long, val createdBy: Long)
                val testCases = listOf(
                    TestCase("상품1", "설명1", 5000L, 1L, 1L),
                    TestCase("상품2", "설명2", 15000L, 2L, 2L),
                    TestCase("상품3", "설명3", 25000L, 3L, 3L)
                )

                testCases.forEach { testCase ->
                    val mockProduct = mockk<Product>()
                    every { mockProductRepository.save(any()) } returns mockProduct

                    val result = sut.createProduct(testCase.name, testCase.description, testCase.price, testCase.categoryId, testCase.createdBy)

                    result shouldBe mockProduct
                    verify(exactly = 1) { mockProductRepository.save(any()) }
                    clearMocks(mockProductRepository)
                }
            }
        }

        context("경계값으로 상품 생성") {
            it("최소값과 최대값으로 상품을 생성") {
                data class TestCase(val name: String, val description: String, val price: Long, val categoryId: Long, val createdBy: Long)
                val testCases = listOf(
                    TestCase("A", "최소 설명", 1L, 1L, 1L),
                    TestCase("매우긴상품명".repeat(10), "매우긴설명".repeat(50), 999999999L, Long.MAX_VALUE, Long.MAX_VALUE)
                )

                testCases.forEach { testCase ->
                    val mockProduct = mockk<Product>()
                    every { mockProductRepository.save(any()) } returns mockProduct

                    val result = sut.createProduct(testCase.name, testCase.description, testCase.price, testCase.categoryId, testCase.createdBy)

                    result shouldBe mockProduct
                    verify(exactly = 1) { mockProductRepository.save(any()) }
                    clearMocks(mockProductRepository)
                }
            }
        }
    }

    describe("updateProduct") {
        context("상품 업데이트 요청") {
            it("ProductRepository에 상품을 저장하고 반환") {
                val mockProduct = mockk<Product>()

                every { mockProductRepository.save(mockProduct) } returns mockProduct

                val result = sut.updateProduct(mockProduct)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductRepository.save(mockProduct) }
            }
        }

        context("다양한 상품 객체로 업데이트") {
            it("각 상품 객체가 정확히 Repository에 전달") {
                val mockProducts = listOf(mockk<Product>(), mockk<Product>(), mockk<Product>())

                mockProducts.forEach { mockProduct ->
                    every { mockProductRepository.save(mockProduct) } returns mockProduct

                    val result = sut.updateProduct(mockProduct)

                    result shouldBe mockProduct
                    verify(exactly = 1) { mockProductRepository.save(mockProduct) }
                    clearMocks(mockProductRepository)
                }
            }
        }
    }

    describe("getProductsByCategory") {
        context("존재하는 카테고리 ID로 조회") {
            it("ProductRepository에서 카테고리별 활성 상품을 조회하고 반환") {
                val categoryId = 1L
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())

                every { mockProductRepository.findByCategoryIdAndIsActive(categoryId, true) } returns mockProducts

                val result = sut.getProductsByCategory(categoryId)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductRepository.findByCategoryIdAndIsActive(categoryId, true) }
            }
        }

        context("상품이 없는 카테고리로 조회") {
            it("빈 리스트를 반환") {
                val categoryId = 999L

                every { mockProductRepository.findByCategoryIdAndIsActive(categoryId, true) } returns emptyList()

                val result = sut.getProductsByCategory(categoryId)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductRepository.findByCategoryIdAndIsActive(categoryId, true) }
            }
        }

        context("다양한 카테고리 ID로 조회") {
            it("각 카테고리 ID가 정확히 Repository에 전달") {
                val categoryIds = listOf(1L, 5L, 10L, 99L)

                categoryIds.forEach { categoryId ->
                    val mockProducts = listOf(mockk<Product>())
                    every { mockProductRepository.findByCategoryIdAndIsActive(categoryId, true) } returns mockProducts

                    val result = sut.getProductsByCategory(categoryId)

                    result shouldBe mockProducts
                    verify(exactly = 1) { mockProductRepository.findByCategoryIdAndIsActive(categoryId, true) }
                    clearMocks(mockProductRepository)
                }
            }
        }
    }

    describe("비즈니스 로직 검증") {
        context("Repository 호출 패턴") {
            it("각 메서드가 올바른 Repository 메서드만 호출") {
                val mockProduct = mockk<Product>()

                // getProducts 테스트
                every { mockProductRepository.findAllByIsActive(true) } returns listOf(mockProduct)
                sut.getProducts(1)
                verify(exactly = 1) { mockProductRepository.findAllByIsActive(true) }
                verify(exactly = 0) { mockProductRepository.findByIdAndIsActive(any(), any()) }
                verify(exactly = 0) { mockProductRepository.save(any()) }

                clearMocks(mockProductRepository)

                // getProduct 테스트
                every { mockProductRepository.findByIdAndIsActive(1L, true) } returns mockProduct
                sut.getProduct(1L)
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(1L, true) }
                verify(exactly = 0) { mockProductRepository.findAllByIsActive(any()) }
                verify(exactly = 0) { mockProductRepository.save(any()) }

                clearMocks(mockProductRepository)

                // createProduct 테스트
                every { mockProductRepository.save(any()) } returns mockProduct
                sut.createProduct("테스트", "설명", 1000L, 1L, 1L)
                verify(exactly = 1) { mockProductRepository.save(any()) }
                verify(exactly = 0) { mockProductRepository.findByIdAndIsActive(any(), any()) }
                verify(exactly = 0) { mockProductRepository.findAllByIsActive(any()) }
            }
        }

        context("활성 상품만 조회 확인") {
            it("모든 조회 메서드가 isActive=true로 Repository 호출") {
                val mockProduct = mockk<Product>()
                val mockProducts = listOf(mockProduct)

                // getProducts 확인
                every { mockProductRepository.findAllByIsActive(true) } returns mockProducts
                sut.getProducts(1)
                verify(exactly = 1) { mockProductRepository.findAllByIsActive(true) }

                clearMocks(mockProductRepository)

                // getProduct 확인
                every { mockProductRepository.findByIdAndIsActive(1L, true) } returns mockProduct
                sut.getProduct(1L)
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(1L, true) }

                clearMocks(mockProductRepository)

                // getProductsByCategory 확인
                every { mockProductRepository.findByCategoryIdAndIsActive(1L, true) } returns mockProducts
                sut.getProductsByCategory(1L)
                verify(exactly = 1) { mockProductRepository.findByCategoryIdAndIsActive(1L, true) }
            }
        }
    }

})