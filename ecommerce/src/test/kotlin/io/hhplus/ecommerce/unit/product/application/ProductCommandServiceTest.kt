package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductCommandService
import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*

/**
 * ProductCommandService 단위 테스트
 *
 * 책임: 상품 명령 서비스의 비즈니스 로직 검증
 * - Repository와의 올바른 상호작용 확인
 * - ProductQueryService와의 연동 확인
 * - 캐시 무효화 검증 (어노테이션 기반)
 * - 예외 처리 검증
 *
 * 검증 목표:
 * 1. Service가 적절한 메서드들을 올바른 순서로 호출하는가?
 * 2. 파라미터를 올바르게 전달하는가?
 * 3. 상태 변경이 올바르게 수행되는가?
 * 4. 예외가 적절하게 전파되는가?
 */
class ProductCommandServiceTest : DescribeSpec({

    val mockProductRepository = mockk<ProductRepository>()
    val mockProductQueryService = mockk<ProductQueryService>()
    val sut = ProductCommandService(mockProductRepository, mockProductQueryService)

    beforeEach {
        clearAllMocks()
    }

    describe("createProduct") {
        context("유효한 상품 정보로 생성할 때") {
            it("새로운 상품을 생성하고 저장한다") {
                // given
                val name = "테스트 상품"
                val description = "테스트 설명"
                val price = 10000L
                val categoryId = 1L
                val savedProduct = mockk<Product>()

                every { mockProductRepository.save(any()) } returns savedProduct

                // when
                val result = sut.createProduct(name, description, price, categoryId)

                // then
                result shouldBe savedProduct
                verify(exactly = 1) { mockProductRepository.save(any()) }
            }
        }

        context("Repository에서 예외가 발생할 때") {
            it("예외를 그대로 전파한다") {
                // given
                val name = "테스트 상품"
                val description = "테스트 설명"
                val price = 10000L
                val categoryId = 1L

                every { mockProductRepository.save(any()) } throws RuntimeException("DB 오류")

                // when & then
                shouldThrow<RuntimeException> {
                    sut.createProduct(name, description, price, categoryId)
                }
            }
        }
    }

    describe("updateProductInfo") {
        context("존재하는 상품의 정보를 수정할 때") {
            it("상품을 조회하고 정보를 수정한 후 저장한다") {
                // given
                val productId = 1L
                val name = "수정된 상품명"
                val description = "수정된 설명"
                val price = 20000L

                val originalProduct = mockk<Product> {
                    every { categoryId } returns 1L
                }
                val updatedProduct = mockk<Product>()

                every { mockProductQueryService.getProduct(productId) } returns originalProduct
                every { originalProduct.updateInfo(name, description, price) } just Runs
                every { mockProductRepository.save(originalProduct) } returns updatedProduct

                // when
                val result = sut.updateProductInfo(productId, name, description, price)

                // then
                result shouldBe updatedProduct
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
                verify(exactly = 1) { originalProduct.updateInfo(name, description, price) }
                verify(exactly = 1) { mockProductRepository.save(originalProduct) }
            }
        }

        context("존재하지 않는 상품 ID로 수정을 시도할 때") {
            it("ProductException.ProductNotFound를 던진다") {
                // given
                val productId = 999L
                val name = "수정된 상품명"
                val description = "수정된 설명"
                val price = 20000L

                every { mockProductQueryService.getProduct(productId) } throws ProductException.ProductNotFound(productId)

                // when & then
                shouldThrow<ProductException.ProductNotFound> {
                    sut.updateProductInfo(productId, name, description, price)
                }
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
                verify(exactly = 0) { mockProductRepository.save(any()) }
            }
        }
    }

    describe("markProductOutOfStock") {
        context("존재하는 상품을 품절 처리할 때") {
            it("상품을 조회하고 품절 처리한 후 저장한다") {
                // given
                val productId = 1L
                val product = mockk<Product> {
                    every { categoryId } returns 1L
                }
                val savedProduct = mockk<Product>()

                every { mockProductQueryService.getProduct(productId) } returns product
                every { product.markOutOfStock() } just Runs
                every { mockProductRepository.save(product) } returns savedProduct

                // when
                val result = sut.markProductOutOfStock(productId)

                // then
                result shouldBe savedProduct
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
                verify(exactly = 1) { product.markOutOfStock() }
                verify(exactly = 1) { mockProductRepository.save(product) }
            }
        }
    }

    describe("discontinueProduct") {
        context("존재하는 상품을 단종 처리할 때") {
            it("상품을 조회하고 단종 처리한 후 저장한다") {
                // given
                val productId = 1L
                val product = mockk<Product> {
                    every { categoryId } returns 1L
                }
                val savedProduct = mockk<Product>()

                every { mockProductQueryService.getProduct(productId) } returns product
                every { product.markDiscontinued() } just Runs
                every { mockProductRepository.save(product) } returns savedProduct

                // when
                val result = sut.discontinueProduct(productId)

                // then
                result shouldBe savedProduct
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
                verify(exactly = 1) { product.markDiscontinued() }
                verify(exactly = 1) { mockProductRepository.save(product) }
            }
        }
    }

    describe("hideProduct") {
        context("존재하는 상품을 숨김 처리할 때") {
            it("상품을 조회하고 숨김 처리한 후 저장한다") {
                // given
                val productId = 1L
                val product = mockk<Product> {
                    every { categoryId } returns 1L
                }
                val savedProduct = mockk<Product>()

                every { mockProductQueryService.getProduct(productId) } returns product
                every { product.hide() } just Runs
                every { mockProductRepository.save(product) } returns savedProduct

                // when
                val result = sut.hideProduct(productId)

                // then
                result shouldBe savedProduct
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
                verify(exactly = 1) { product.hide() }
                verify(exactly = 1) { mockProductRepository.save(product) }
            }
        }
    }

    describe("restoreProduct") {
        context("존재하는 상품을 복구 처리할 때") {
            it("상품을 조회하고 복구 처리한 후 저장한다") {
                // given
                val productId = 1L
                val product = mockk<Product> {
                    every { categoryId } returns 1L
                }
                val savedProduct = mockk<Product>()

                every { mockProductQueryService.getProduct(productId) } returns product
                every { product.restore() } just Runs
                every { mockProductRepository.save(product) } returns savedProduct

                // when
                val result = sut.restoreProduct(productId)

                // then
                result shouldBe savedProduct
                verify(exactly = 1) { mockProductQueryService.getProduct(productId) }
                verify(exactly = 1) { product.restore() }
                verify(exactly = 1) { mockProductRepository.save(product) }
            }
        }
    }

    describe("updateProduct") {
        context("상품 객체를 직접 업데이트할 때") {
            it("상품을 저장한다") {
                // given
                val product = mockk<Product> {
                    every { id } returns 1L
                    every { categoryId } returns 1L
                }
                val savedProduct = mockk<Product>()

                every { mockProductRepository.save(product) } returns savedProduct

                // when
                val result = sut.updateProduct(product)

                // then
                result shouldBe savedProduct
                verify(exactly = 1) { mockProductRepository.save(product) }
            }
        }
    }
})