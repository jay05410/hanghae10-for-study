package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import io.hhplus.ecommerce.common.response.Cursor
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*

/**
 * ProductService 단위 테스트
 *
 * 책임: 상품 도메인 비즈니스 로직 처리 검증
 * - 상품 생성/조회/수정 로직 검증
 * - 커서 기반 페이징 로직 검증
 * - ProductRepository와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. 각 비즈니스 메서드가 적절한 Repository 메서드를 호출하는가?
 * 2. 존재하지 않는 상품 접근 시 적절한 예외가 발생하는가?
 * 3. 상품 생성 시 도메인 객체가 올바르게 생성되는가?
 * 4. 커서 기반 페이징 로직이 올바르게 처리되는가?
 * 5. 각 메서드의 반환값이 올바른가?
 */
class ProductServiceTest : DescribeSpec({
    val mockProductRepository = mockk<ProductRepository>()
    val sut = ProductService(mockProductRepository)

    beforeEach {
        clearMocks(mockProductRepository)
    }

    describe("getProductsWithCursor") {
        context("커서 기반 상품 목록 조회") {
            it("ProductRepository에서 활성 상품을 조회하고 Cursor를 반환") {
                val lastId = null
                val size = 20
                val mockProducts = listOf(mockk<Product>(), mockk<Product>(), mockk<Product>())

                every { mockProductRepository.findActiveProductsWithCursor(any(), any()) } returns mockProducts

                val result = sut.getProductsWithCursor(lastId, size)

                result.contents shouldBe mockProducts
                verify(exactly = 1) { mockProductRepository.findActiveProductsWithCursor(lastId, size + 1) }
            }
        }

        context("다음 페이지 존재하는 경우") {
            it("hasNext가 true이고 nextLastId가 설정됨") {
                val size = 2
                val mockProduct1 = mockk<Product> { every { id } returns 1L }
                val mockProduct2 = mockk<Product> { every { id } returns 2L }
                val mockProduct3 = mockk<Product> { every { id } returns 3L }
                val mockProducts = listOf(mockProduct1, mockProduct2, mockProduct3)

                every { mockProductRepository.findActiveProductsWithCursor(any(), any()) } returns mockProducts

                val result = sut.getProductsWithCursor(null, size)

                result.contents.size shouldBe 2
                result.lastId shouldBe 2L
                verify(exactly = 1) { mockProductRepository.findActiveProductsWithCursor(null, 3) }
            }
        }
    }

    describe("getProduct") {
        context("존재하는 상품 조회") {
            it("Repository에서 상품을 조회하여 반환") {
                val productId = 1L
                val mockProduct = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(productId) } returns mockProduct

                val result = sut.getProduct(productId)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
            }
        }

        context("존재하지 않는 상품 조회") {
            it("ProductNotFound 예외 발생") {
                val productId = 999L

                every { mockProductRepository.findByIdAndIsActive(productId) } returns null

                shouldThrow<ProductException.ProductNotFound> {
                    sut.getProduct(productId)
                }

                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
            }
        }
    }

    describe("createProduct") {
        context("새로운 상품 생성") {
            it("상품을 생성하고 저장하여 반환") {
                val name = "테스트 상품"
                val description = "테스트 설명"
                val price = 10000L
                val categoryId = 1L
                val mockProduct = mockk<Product>()

                every { mockProductRepository.save(any()) } returns mockProduct

                val result = sut.createProduct(name, description, price, categoryId)

                result shouldBe mockProduct
                verify(exactly = 1) { mockProductRepository.save(any()) }
            }
        }
    }

    describe("updateProductInfo") {
        context("상품 정보 업데이트") {
            it("상품을 조회하여 정보를 업데이트하고 저장") {
                val productId = 1L
                val name = "수정된 상품"
                val description = "수정된 설명"
                val price = 15000L
                val mockProduct = mockk<Product>(relaxed = true)
                val updatedProduct = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(productId) } returns mockProduct
                every { mockProductRepository.save(mockProduct) } returns updatedProduct

                val result = sut.updateProductInfo(productId, name, description, price)

                result shouldBe updatedProduct
                verify(exactly = 1) { mockProduct.updateInfo(name, description, price) }
                verify(exactly = 1) { mockProductRepository.save(mockProduct) }
            }
        }
    }

    describe("markProductOutOfStock") {
        context("상품 품절 처리") {
            it("상품을 조회하여 품절로 마킹하고 저장") {
                val productId = 1L
                val mockProduct = mockk<Product>(relaxed = true)
                val updatedProduct = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(productId) } returns mockProduct
                every { mockProductRepository.save(mockProduct) } returns updatedProduct

                val result = sut.markProductOutOfStock(productId)

                result shouldBe updatedProduct
                verify(exactly = 1) { mockProduct.markOutOfStock() }
                verify(exactly = 1) { mockProductRepository.save(mockProduct) }
            }
        }
    }

    describe("getProductsByCategoryWithCursor") {
        context("카테고리별 상품 조회 (커서 기반)") {
            it("카테고리별 상품을 조회하고 Cursor를 반환") {
                val categoryId = 1L
                val lastId = null
                val size = 20
                val mockProducts = listOf(mockk<Product>(), mockk<Product>())

                every { mockProductRepository.findCategoryProductsWithCursor(categoryId, lastId, size + 1) } returns mockProducts

                val result = sut.getProductsByCategoryWithCursor(categoryId, lastId, size)

                result.contents shouldBe mockProducts
                verify(exactly = 1) { mockProductRepository.findCategoryProductsWithCursor(categoryId, lastId, size + 1) }
            }
        }
    }
})