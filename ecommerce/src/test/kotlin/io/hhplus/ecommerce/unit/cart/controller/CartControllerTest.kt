package io.hhplus.ecommerce.unit.cart.controller

import io.hhplus.ecommerce.cart.controller.CartController
import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.usecase.GetCartUseCase
import io.hhplus.ecommerce.cart.usecase.CartCommandUseCase
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CartController 단위 테스트
 *
 * 책임: 장바구니 관련 HTTP 요청 처리 검증
 * - REST API 엔드포인트의 요청/응답 처리 검증
 * - UseCase 계층과의 올바른 상호작용 검증
 * - 요청 데이터 변환 및 응답 형식 검증
 *
 * 검증 목표:
 * 1. 각 엔드포인트가 적절한 UseCase를 호출하는가?
 * 2. 요청 파라미터와 Body가 올바르게 UseCase에 전달되는가?
 * 3. UseCase 결과가 적절한 ApiResponse로 변환되는가?
 * 4. HTTP 메서드와 경로 매핑이 올바른가?
 * 5. 다양한 요청 형태에 대한 처리가 올바른가?
 */
class CartControllerTest : DescribeSpec({
    val mockGetCartUseCase = mockk<GetCartUseCase>()
    val mockCartCommandUseCase = mockk<CartCommandUseCase>()

    val sut = CartController(
        getCartUseCase = mockGetCartUseCase,
        cartCommandUseCase = mockCartCommandUseCase
    )

    beforeEach {
        clearMocks(
            mockGetCartUseCase,
            mockCartCommandUseCase
        )
    }

    describe("getCart") {
        context("GET /api/v1/cart?userId={userId} 요청") {
            it("GetCartUseCase를 호출하고 ApiResponse로 감싸서 반환") {
                val userId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockGetCartUseCase.execute(userId) } returns mockCart

                val result = sut.getCart(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetCartUseCase.execute(userId) }
            }
        }

        context("장바구니가 없는 사용자 조회") {
            it("UseCase에서 반환된 null을 ApiResponse로 감싸서 반환") {
                val userId = 999L

                every { mockGetCartUseCase.execute(userId) } returns null

                val result = sut.getCart(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetCartUseCase.execute(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("요청된 userId를 정확히 UseCase에 전달") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockCart = mockk<Cart>(relaxed = true)
                    every { mockGetCartUseCase.execute(userId) } returns mockCart

                    val result = sut.getCart(userId)

                    result.success shouldBe true
                    verify(exactly = 1) { mockGetCartUseCase.execute(userId) }
                    clearMocks(mockGetCartUseCase)
                }
            }
        }
    }

    describe("addToCart") {
        context("POST /api/v1/cart/items 요청") {
            it("AddToCartRequest를 AddToCartUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val request = AddToCartRequest(
                    productId = 1L,
                    quantity = 2,
                    giftWrap = false,
                    giftMessage = null
                )
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.addToCart(userId, request) } returns mockCart

                val result = sut.addToCart(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.addToCart(userId, request) }
            }
        }

        context("다른 상품 추가") {
            it("다른 상품 ID로 요청을 정확히 UseCase에 전달") {
                val userId = 2L
                val request = AddToCartRequest(
                    productId = 2L,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                )
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.addToCart(userId, request) } returns mockCart

                val result = sut.addToCart(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.addToCart(userId, request) }
            }
        }

        context("선물 포장 상품 추가") {
            it("선물 포장을 포함한 요청을 UseCase에 전달") {
                val userId = 3L
                val request = AddToCartRequest(
                    productId = 3L,
                    quantity = 1,
                    giftWrap = true,
                    giftMessage = "선물메시지"
                )
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.addToCart(userId, request) } returns mockCart

                val result = sut.addToCart(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.addToCart(userId, request) }
            }
        }
    }

    describe("updateCartItem") {
        context("PUT /api/v1/cart/items/{cartItemId} 요청") {
            it("파라미터들을 UpdateCartItemUseCase에 전달하고 ApiResponse로 반환") {
                val cartItemId = 1L
                val userId = 1L
                val quantity = 500
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.updateCartItem(userId, cartItemId, quantity) } returns mockCart

                val result = sut.updateCartItem(cartItemId, userId, quantity)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.updateCartItem(userId, cartItemId, quantity) }
            }
        }

        context("수량을 0으로 수정") {
            it("0 수량을 포함한 파라미터를 UseCase에 전달") {
                val cartItemId = 2L
                val userId = 2L
                val quantity = 0
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.updateCartItem(userId, cartItemId, quantity) } returns mockCart

                val result = sut.updateCartItem(cartItemId, userId, quantity)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.updateCartItem(userId, cartItemId, quantity) }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 UseCase에 전달되는지 확인") {
                val testCases = listOf(
                    Triple(1L, 1L, 100),
                    Triple(100L, 200L, 1000),
                    Triple(999L, 888L, 9990)
                )

                testCases.forEach { (cartItemId, userId, quantity) ->
                    val mockCart = mockk<Cart>(relaxed = true)
                    every { mockCartCommandUseCase.updateCartItem(userId, cartItemId, quantity) } returns mockCart

                    val result = sut.updateCartItem(cartItemId, userId, quantity)

                    result.success shouldBe true
                    verify(exactly = 1) { mockCartCommandUseCase.updateCartItem(userId, cartItemId, quantity) }
                    clearMocks(mockCartCommandUseCase)
                }
            }
        }
    }

    describe("removeCartItem") {
        context("DELETE /api/v1/cart/items/{cartItemId} 요청") {
            it("파라미터들을 RemoveCartItemUseCase에 전달하고 ApiResponse로 반환") {
                val cartItemId = 1L
                val userId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.removeCartItem(userId, cartItemId) } returns mockCart

                val result = sut.removeCartItem(cartItemId, userId)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.removeCartItem(userId, cartItemId) }
            }
        }

        context("다양한 사용자와 아이템 조합") {
            it("각각의 파라미터가 정확히 UseCase에 전달되는지 확인") {
                val testCases = listOf(
                    Pair(1L, 1L),
                    Pair(100L, 200L),
                    Pair(999L, 888L)
                )

                testCases.forEach { (cartItemId, userId) ->
                    val mockCart = mockk<Cart>(relaxed = true)
                    every { mockCartCommandUseCase.removeCartItem(userId, cartItemId) } returns mockCart

                    val result = sut.removeCartItem(cartItemId, userId)

                    result.success shouldBe true
                    verify(exactly = 1) { mockCartCommandUseCase.removeCartItem(userId, cartItemId) }
                    clearMocks(mockCartCommandUseCase)
                }
            }
        }
    }

    describe("clearCart") {
        context("DELETE /api/v1/cart?userId={userId} 요청") {
            it("userId를 ClearCartUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartCommandUseCase.clearCart(userId) } returns mockCart

                val result = sut.clearCart(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockCartCommandUseCase.clearCart(userId) }
            }
        }

        context("다양한 사용자의 장바구니 비우기") {
            it("각각의 userId가 정확히 UseCase에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockCart = mockk<Cart>(relaxed = true)
                    every { mockCartCommandUseCase.clearCart(userId) } returns mockCart

                    val result = sut.clearCart(userId)

                    result.success shouldBe true
                    verify(exactly = 1) { mockCartCommandUseCase.clearCart(userId) }
                    clearMocks(mockCartCommandUseCase)
                }
            }
        }
    }

    describe("API 경로 및 메서드 검증") {
        context("모든 엔드포인트") {
            it("적절한 UseCase만 호출하고 다른 UseCase는 호출하지 않음") {
                // getCart 테스트
                every { mockGetCartUseCase.execute(1L) } returns mockk(relaxed = true)
                sut.getCart(1L)
                verify(exactly = 1) { mockGetCartUseCase.execute(1L) }
                verify(exactly = 0) { mockCartCommandUseCase.addToCart(any(), any()) }
                verify(exactly = 0) { mockCartCommandUseCase.updateCartItem(any(), any(), any()) }

                clearMocks(mockGetCartUseCase, mockCartCommandUseCase)

                // addToCart 테스트
                val addRequest = AddToCartRequest(
                    productId = 1L,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                )
                every { mockCartCommandUseCase.addToCart(1L, addRequest) } returns mockk(relaxed = true)
                sut.addToCart(1L, addRequest)
                verify(exactly = 1) { mockCartCommandUseCase.addToCart(1L, addRequest) }
                verify(exactly = 0) { mockGetCartUseCase.execute(any()) }
                verify(exactly = 0) { mockCartCommandUseCase.updateCartItem(any(), any(), any()) }

                clearMocks(mockGetCartUseCase, mockCartCommandUseCase)

                // updateCartItem 테스트
                every { mockCartCommandUseCase.updateCartItem(1L, 1L, 1) } returns mockk(relaxed = true)
                sut.updateCartItem(1L, 1L, 1)
                verify(exactly = 1) { mockCartCommandUseCase.updateCartItem(1L, 1L, 1) }
                verify(exactly = 0) { mockGetCartUseCase.execute(any()) }
                verify(exactly = 0) { mockCartCommandUseCase.addToCart(any(), any()) }
            }
        }
    }
})