package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import io.hhplus.ecommerce.common.response.Cursor
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*

/**
 * ProductQueryService 단위 테스트
 *
 * 책임: 상품 조회 서비스의 비즈니스 로직 검증
 * - Repository와의 올바른 상호작용 확인
 * - 캐싱 로직 검증 (어노테이션 기반)
 * - 예외 처리 검증
 *
 * 검증 목표:
 * 1. Service가 적절한 Repository 메서드를 호출하는가?
 * 2. 파라미터를 올바르게 전달하는가?
 * 3. 결과를 올바르게 반환하는가?
 * 4. 예외가 Repository에서 Service로 전파되는가?
 */
class ProductQueryServiceTest : DescribeSpec({

    val mockProductRepository = mockk<ProductRepository>()
    val sut = ProductQueryService(mockProductRepository)

    beforeEach {
        clearAllMocks()
    }

    describe("getProduct") {
        context("존재하는 상품 ID로 조회할 때") {
            it("해당 상품을 반환한다") {
                // given
                val productId = 1L
                val expectedProduct = mockk<Product>()
                every { mockProductRepository.findByIdAndIsActive(productId) } returns expectedProduct

                // when
                val result = sut.getProduct(productId)

                // then
                result shouldBe expectedProduct
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
            }
        }

        context("존재하지 않는 상품 ID로 조회할 때") {
            it("ProductException.ProductNotFound를 던진다") {
                // given
                val productId = 999L
                every { mockProductRepository.findByIdAndIsActive(productId) } returns null

                // when & then
                shouldThrow<ProductException.ProductNotFound> {
                    sut.getProduct(productId)
                }
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
            }
        }
    }

    describe("getProductsWithCursor") {
        context("커서와 사이즈로 상품 목록을 조회할 때") {
            it("Repository에서 반환된 결과로 Cursor를 생성한다") {
                // given
                val lastId = 10L
                val size = 5
                val products = (1..5).map { mockk<Product>() }
                every { mockProductRepository.findActiveProductsWithCursor(lastId, size + 1) } returns products

                // when
                val result = sut.getProductsWithCursor(lastId, size)

                // then
                result.contents.size shouldBe 5
                result.lastId shouldBe null // hasNext = false인 경우
                verify(exactly = 1) { mockProductRepository.findActiveProductsWithCursor(lastId, size + 1) }
            }
        }

        context("다음 페이지가 있는 경우") {
            it("hasNext가 true인 Cursor를 반환한다") {
                // given
                val lastId = 10L
                val size = 5
                val products = (1..6).map {
                    mockk<Product> {
                        every { id } returns it.toLong()
                    }
                }
                every { mockProductRepository.findActiveProductsWithCursor(lastId, size + 1) } returns products

                // when
                val result = sut.getProductsWithCursor(lastId, size)

                // then
                result.contents.size shouldBe 5
                result.lastId shouldBe 5L
                verify(exactly = 1) { mockProductRepository.findActiveProductsWithCursor(lastId, size + 1) }
            }
        }
    }

    describe("getProductsByCategoryWithCursor") {
        context("카테고리 ID로 상품 목록을 조회할 때") {
            it("해당 카테고리의 상품 목록을 Cursor로 반환한다") {
                // given
                val categoryId = 1L
                val lastId = 10L
                val size = 5
                val products = (1..3).map { mockk<Product>() }
                every { mockProductRepository.findCategoryProductsWithCursor(categoryId, lastId, size + 1) } returns products

                // when
                val result = sut.getProductsByCategoryWithCursor(categoryId, lastId, size)

                // then
                result.contents.size shouldBe 3
                result.lastId shouldBe null
                verify(exactly = 1) { mockProductRepository.findCategoryProductsWithCursor(categoryId, lastId, size + 1) }
            }
        }
    }

    describe("getActiveProductsByCategory") {
        context("카테고리 ID로 활성 상품들을 조회할 때") {
            it("해당 카테고리의 모든 활성 상품을 반환한다") {
                // given
                val categoryId = 1L
                val expectedProducts = listOf(mockk<Product>(), mockk<Product>())
                every { mockProductRepository.findActiveProductsByCategory(categoryId) } returns expectedProducts

                // when
                val result = sut.getActiveProductsByCategory(categoryId)

                // then
                result shouldBe expectedProducts
                verify(exactly = 1) { mockProductRepository.findActiveProductsByCategory(categoryId) }
            }
        }
    }

    describe("getProducts") {
        context("여러 상품 ID 목록으로 조회할 때") {
            it("존재하는 상품들만 반환한다") {
                // given
                val productIds = listOf(1L, 2L, 3L)
                val product1 = mockk<Product>()
                val product2 = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(1L) } returns product1
                every { mockProductRepository.findByIdAndIsActive(2L) } returns product2
                every { mockProductRepository.findByIdAndIsActive(3L) } returns null

                // when
                val result = sut.getProducts(productIds)

                // then
                result.size shouldBe 2
                result shouldBe listOf(product1, product2)
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(1L) }
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(2L) }
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(3L) }
            }
        }

        context("존재하지 않는 상품 ID들만 있을 때") {
            it("빈 목록을 반환한다") {
                // given
                val productIds = listOf(999L, 998L)
                every { mockProductRepository.findByIdAndIsActive(any()) } returns null

                // when
                val result = sut.getProducts(productIds)

                // then
                result shouldBe emptyList()
                verify(exactly = 2) { mockProductRepository.findByIdAndIsActive(any()) }
            }
        }
    }
})