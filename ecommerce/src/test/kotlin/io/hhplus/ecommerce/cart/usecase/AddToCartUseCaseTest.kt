package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * AddToCartUseCase 단위 테스트
 *
 * 책임: 장바구니 상품 추가 비즈니스 흐름 검증
 * - 상품 존재 여부 검증과 장바구니 추가 프로세스의 서비스 조합 검증
 * - 비즈니스 로직 순서 및 파라미터 전달 검증
 *
 * 검증 목표:
 * 1. 상품 존재 여부 검증이 먼저 수행되는가?
 * 2. 검증 후 CartService에 올바른 파라미터가 전달되는가?
 * 3. 각 서비스 호출 순서가 올바른가?
 * 4. 다양한 요청 데이터에 대해 정확히 처리되는가?
 */
class AddToCartUseCaseTest : DescribeSpec({
    val mockCartService = mockk<CartService>()
    val mockProductService = mockk<ProductService>()
    val sut = AddToCartUseCase(mockCartService, mockProductService)

    beforeEach {
        clearMocks(mockCartService, mockProductService)
    }

    describe("execute") {
        context("정상적인 상품 추가") {
            it("상품을 검증하고 장바구니에 추가") {
                val userId = 1L
                val teaItems = listOf(TeaItemRequest(productId = 2L, quantity = 1))
                val request = AddToCartRequest(
                    productId = 1L,
                    boxTypeId = 1L,
                    quantity = 2,
                    teaItems = teaItems
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(1L) } returns mockProduct
                every { mockCartService.addToCart(userId, 1L, 1L, 2, teaItems) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verifyOrder {
                    mockProductService.getProduct(1L)
                    mockCartService.addToCart(userId, 1L, 1L, 2, teaItems)
                }
            }
        }

        context("차 구성이 없는 상품 추가") {
            it("빈 차 구성 리스트로 장바구니에 추가") {
                val userId = 2L
                val request = AddToCartRequest(
                    productId = 3L,
                    boxTypeId = 2L,
                    quantity = 1,
                    teaItems = emptyList()
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(3L) } returns mockProduct
                every { mockCartService.addToCart(userId, 3L, 2L, 1, emptyList()) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verifyOrder {
                    mockProductService.getProduct(3L)
                    mockCartService.addToCart(userId, 3L, 2L, 1, emptyList())
                }
            }
        }

        context("복잡한 차 구성이 있는 상품 추가") {
            it("여러 차 구성을 포함하여 장바구니에 추가") {
                val userId = 3L
                val teaItems = listOf(
                    TeaItemRequest(productId = 10L, quantity = 2),
                    TeaItemRequest(productId = 11L, quantity = 1),
                    TeaItemRequest(productId = 12L, quantity = 3)
                )
                val request = AddToCartRequest(
                    productId = 5L,
                    boxTypeId = 3L,
                    quantity = 1,
                    teaItems = teaItems
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(5L) } returns mockProduct
                every { mockCartService.addToCart(userId, 5L, 3L, 1, teaItems) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verify(exactly = 1) { mockProductService.getProduct(5L) }
                verify(exactly = 1) { mockCartService.addToCart(userId, 5L, 3L, 1, teaItems) }
            }
        }

        context("다양한 요청 파라미터 검증") {
            it("모든 요청 파라미터가 정확히 서비스에 전달되는지 확인") {
                val userId = 100L
                val productId = 200L
                val boxTypeId = 300L
                val quantity = 5
                val teaItems = listOf(TeaItemRequest(productId = 400L, quantity = 2))
                val request = AddToCartRequest(
                    productId = productId,
                    boxTypeId = boxTypeId,
                    quantity = quantity,
                    teaItems = teaItems
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockCartService.addToCart(userId, productId, boxTypeId, quantity, teaItems) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verify(exactly = 1) { mockProductService.getProduct(productId) }
                verify(exactly = 1) { mockCartService.addToCart(userId, productId, boxTypeId, quantity, teaItems) }
            }
        }

        context("상품 ID만 다른 여러 요청") {
            it("각각 다른 상품에 대해 정확한 검증을 수행") {
                val userId = 1L
                val request1 = AddToCartRequest(productId = 1L, boxTypeId = 1L, quantity = 1, teaItems = emptyList())
                val request2 = AddToCartRequest(productId = 2L, boxTypeId = 1L, quantity = 1, teaItems = emptyList())

                val mockProduct1 = mockk<Product>()
                val mockProduct2 = mockk<Product>()
                val mockCart1 = mockk<Cart>()
                val mockCart2 = mockk<Cart>()

                every { mockProductService.getProduct(1L) } returns mockProduct1
                every { mockProductService.getProduct(2L) } returns mockProduct2
                every { mockCartService.addToCart(userId, 1L, 1L, 1, emptyList()) } returns mockCart1
                every { mockCartService.addToCart(userId, 2L, 1L, 1, emptyList()) } returns mockCart2

                val result1 = sut.execute(userId, request1)
                val result2 = sut.execute(userId, request2)

                result1 shouldBe mockCart1
                result2 shouldBe mockCart2
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
                verify(exactly = 1) { mockCartService.addToCart(userId, 1L, 1L, 1, emptyList()) }
                verify(exactly = 1) { mockCartService.addToCart(userId, 2L, 1L, 1, emptyList()) }
            }
        }
    }
})