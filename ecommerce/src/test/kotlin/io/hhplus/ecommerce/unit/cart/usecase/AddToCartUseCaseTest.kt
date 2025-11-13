package io.hhplus.ecommerce.unit.cart.usecase

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
        context("정상적인 패키지 추가") {
            it("패키지를 검증하고 장바구니에 추가") {
                val userId = 1L
                val teaItems = listOf(TeaItemRequest(productId = 2L, selectionOrder = 1, ratioPercent = 100))
                val request = AddToCartRequest(
                    packageTypeId = 1L,
                    packageTypeName = "30일 패키지",
                    packageTypeDays = 30,
                    dailyServing = 2,
                    totalQuantity = 300.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = teaItems
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(1L) } returns mockProduct
                every { mockCartService.addToCart(userId, 1L, "30일 패키지", 30, 2, 300.0, false, null, teaItems) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verifyOrder {
                    mockProductService.getProduct(1L)
                    mockCartService.addToCart(userId, 1L, "30일 패키지", 30, 2, 300.0, false, null, teaItems)
                }
            }
        }

        context("차 구성이 없는 패키지 추가") {
            it("빈 차 구성 리스트로 장바구니에 추가") {
                val userId = 2L
                val request = AddToCartRequest(
                    packageTypeId = 2L,
                    packageTypeName = "15일 패키지",
                    packageTypeDays = 15,
                    dailyServing = 1,
                    totalQuantity = 150.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = emptyList()
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(2L) } returns mockProduct
                every { mockCartService.addToCart(userId, 2L, "15일 패키지", 15, 1, 150.0, false, null, emptyList()) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verifyOrder {
                    mockProductService.getProduct(2L)
                    mockCartService.addToCart(userId, 2L, "15일 패키지", 15, 1, 150.0, false, null, emptyList())
                }
            }
        }

        context("복잡한 차 구성이 있는 패키지 추가") {
            it("여러 차 구성을 포함하여 장바구니에 추가") {
                val userId = 3L
                val teaItems = listOf(
                    TeaItemRequest(productId = 10L, selectionOrder = 1, ratioPercent = 40),
                    TeaItemRequest(productId = 11L, selectionOrder = 2, ratioPercent = 30),
                    TeaItemRequest(productId = 12L, selectionOrder = 3, ratioPercent = 30)
                )
                val request = AddToCartRequest(
                    packageTypeId = 3L,
                    packageTypeName = "7일 패키지",
                    packageTypeDays = 7,
                    dailyServing = 3,
                    totalQuantity = 70.0,
                    giftWrap = true,
                    giftMessage = "선물메시지",
                    teaItems = teaItems
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(3L) } returns mockProduct
                every { mockCartService.addToCart(userId, 3L, "7일 패키지", 7, 3, 70.0, true, "선물메시지", teaItems) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verify(exactly = 1) { mockProductService.getProduct(3L) }
                verify(exactly = 1) { mockCartService.addToCart(userId, 3L, "7일 패키지", 7, 3, 70.0, true, "선물메시지", teaItems) }
            }
        }

        context("다양한 요청 파라미터 검증") {
            it("모든 요청 파라미터가 정확히 서비스에 전달되는지 확인") {
                val userId = 100L
                val packageTypeId = 200L
                val packageTypeName = "60일 패키지"
                val packageTypeDays = 60
                val dailyServing = 2
                val totalQuantity = 600.0
                val giftWrap = true
                val giftMessage = "특별한 선물"
                val teaItems = listOf(TeaItemRequest(productId = 400L, selectionOrder = 1, ratioPercent = 100))
                val request = AddToCartRequest(
                    packageTypeId = packageTypeId,
                    packageTypeName = packageTypeName,
                    packageTypeDays = packageTypeDays,
                    dailyServing = dailyServing,
                    totalQuantity = totalQuantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage,
                    teaItems = teaItems
                )
                val mockProduct = mockk<Product>()
                val mockCart = mockk<Cart>()

                every { mockProductService.getProduct(packageTypeId) } returns mockProduct
                every { mockCartService.addToCart(userId, packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, teaItems) } returns mockCart

                val result = sut.execute(userId, request)

                result shouldBe mockCart
                verify(exactly = 1) { mockProductService.getProduct(packageTypeId) }
                verify(exactly = 1) { mockCartService.addToCart(userId, packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, teaItems) }
            }
        }

        context("패키지 ID만 다른 여러 요청") {
            it("각각 다른 패키지에 대해 정확한 검증을 수행") {
                val userId = 1L
                val request1 = AddToCartRequest(
                    packageTypeId = 1L, packageTypeName = "30일", packageTypeDays = 30,
                    dailyServing = 1, totalQuantity = 300.0, giftWrap = false, giftMessage = null, teaItems = emptyList()
                )
                val request2 = AddToCartRequest(
                    packageTypeId = 2L, packageTypeName = "15일", packageTypeDays = 15,
                    dailyServing = 1, totalQuantity = 150.0, giftWrap = false, giftMessage = null, teaItems = emptyList()
                )

                val mockProduct1 = mockk<Product>()
                val mockProduct2 = mockk<Product>()
                val mockCart1 = mockk<Cart>()
                val mockCart2 = mockk<Cart>()

                every { mockProductService.getProduct(1L) } returns mockProduct1
                every { mockProductService.getProduct(2L) } returns mockProduct2
                every { mockCartService.addToCart(userId, 1L, "30일", 30, 1, 300.0, false, null, emptyList()) } returns mockCart1
                every { mockCartService.addToCart(userId, 2L, "15일", 15, 1, 150.0, false, null, emptyList()) } returns mockCart2

                val result1 = sut.execute(userId, request1)
                val result2 = sut.execute(userId, request2)

                result1 shouldBe mockCart1
                result2 shouldBe mockCart2
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
                verify(exactly = 1) { mockCartService.addToCart(userId, 1L, "30일", 30, 1, 300.0, false, null, emptyList()) }
                verify(exactly = 1) { mockCartService.addToCart(userId, 2L, "15일", 15, 1, 150.0, false, null, emptyList()) }
            }
        }
    }
})