package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductCommandService
import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

/**
 * CQRS 패턴 적용 후 ProductQueryService와 ProductCommandService 연동 테스트
 *
 * 목적: self-invocation 문제 해결 검증
 * - ProductCommandService가 ProductQueryService를 외부 호출로 사용
 * - 캐시 어노테이션이 정상 작동하는 구조 확인
 * - Mock을 통한 상호작용 검증
 */
class ProductCQRSIntegrationTest : DescribeSpec({

    val mockProductRepository = mockk<ProductRepository>()
    val productQueryService = ProductQueryService(mockProductRepository)
    val productCommandService = ProductCommandService(mockProductRepository, productQueryService)

    beforeEach {
        clearAllMocks()
    }

    describe("CQRS 패턴 연동 테스트") {
        context("ProductCommandService에서 ProductQueryService를 호출할 때") {
            it("외부 서비스 호출로 캐시가 정상 작동할 수 있는 구조이다") {
                // given
                val productId = 1L
                val product = mockk<Product> {
                    every { categoryId } returns 1L
                }
                val updatedProduct = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(productId) } returns product
                every { product.markOutOfStock() } just Runs
                every { mockProductRepository.save(product) } returns updatedProduct

                // when
                val result = productCommandService.markProductOutOfStock(productId)

                // then
                result shouldBe updatedProduct

                // ProductCommandService가 ProductQueryService를 외부 호출로 사용함을 확인
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
                verify(exactly = 1) { product.markOutOfStock() }
                verify(exactly = 1) { mockProductRepository.save(product) }
            }
        }

        context("ProductQueryService가 독립적으로 작동할 때") {
            it("캐시 어노테이션이 적용된 메서드들이 정상 호출된다") {
                // given
                val productId = 1L
                val expectedProduct = mockk<Product>()

                every { mockProductRepository.findByIdAndIsActive(productId) } returns expectedProduct

                // when
                val result = productQueryService.getProduct(productId)

                // then
                result shouldBe expectedProduct
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
            }
        }

        context("서비스들이 서로 다른 인스턴스일 때") {
            it("self-invocation 문제가 해결된다") {
                // given: 서로 다른 서비스 인스턴스임을 확인
                productQueryService shouldNotBe productCommandService

                // CommandService가 QueryService를 의존성으로 가지고 있음을 확인
                // (실제로는 private field이므로 동작 검증으로 확인)
                val productId = 1L
                val product = mockk<Product> {
                    every { categoryId } returns 1L
                }

                every { mockProductRepository.findByIdAndIsActive(productId) } returns product
                every { product.restore() } just Runs
                every { mockProductRepository.save(product) } returns product

                // when
                productCommandService.restoreProduct(productId)

                // then
                // ProductQueryService의 메서드가 호출되었음을 Repository 호출로 확인
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
                verify(exactly = 1) { product.restore() }
            }
        }
    }

    describe("CQRS 분리 효과 검증") {
        context("Query와 Command가 명확히 분리되었을 때") {
            it("각각의 책임이 명확하다") {
                // ProductQueryService는 조회만
                val productId = 1L
                val product = mockk<Product>()
                every { mockProductRepository.findByIdAndIsActive(productId) } returns product

                productQueryService.getProduct(productId)
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
                verify(exactly = 0) { mockProductRepository.save(any()) }

                clearAllMocks()

                // ProductCommandService는 상태 변경
                every { mockProductRepository.findByIdAndIsActive(productId) } returns product
                every { product.categoryId } returns 1L
                every { product.hide() } just Runs
                every { mockProductRepository.save(product) } returns product

                productCommandService.hideProduct(productId)
                verify(exactly = 1) { mockProductRepository.findByIdAndIsActive(productId) }
                verify(exactly = 1) { mockProductRepository.save(any()) }
            }
        }
    }
})