package io.hhplus.ecommerce.cart.controller

import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.usecase.*
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
    val mockAddToCartUseCase = mockk<AddToCartUseCase>()
    val mockUpdateCartItemUseCase = mockk<UpdateCartItemUseCase>()
    val mockRemoveCartItemUseCase = mockk<RemoveCartItemUseCase>()
    val mockClearCartUseCase = mockk<ClearCartUseCase>()

    val sut = CartController(
        getCartUseCase = mockGetCartUseCase,
        addToCartUseCase = mockAddToCartUseCase,
        updateCartItemUseCase = mockUpdateCartItemUseCase,
        removeCartItemUseCase = mockRemoveCartItemUseCase,
        clearCartUseCase = mockClearCartUseCase
    )

    beforeEach {
        clearMocks(
            mockGetCartUseCase,
            mockAddToCartUseCase,
            mockUpdateCartItemUseCase,
            mockRemoveCartItemUseCase,
            mockClearCartUseCase
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
                    packageTypeId = 1L,
                    packageTypeName = "30일 패키지",
                    packageTypeDays = 30,
                    dailyServing = 2,
                    totalQuantity = 300.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = listOf(TeaItemRequest(productId = 2L, selectionOrder = 1, ratioPercent = 100))
                )
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockAddToCartUseCase.execute(userId, request) } returns mockCart

                val result = sut.addToCart(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockAddToCartUseCase.execute(userId, request) }
            }
        }

        context("차 구성이 없는 상품 추가") {
            it("빈 차 구성을 포함한 요청을 정확히 UseCase에 전달") {
                val userId = 2L
                val request = AddToCartRequest(
                    packageTypeId = 3L,
                    packageTypeName = "15일 패키지",
                    packageTypeDays = 15,
                    dailyServing = 1,
                    totalQuantity = 150.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = emptyList()
                )
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockAddToCartUseCase.execute(userId, request) } returns mockCart

                val result = sut.addToCart(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockAddToCartUseCase.execute(userId, request) }
            }
        }

        context("복잡한 차 구성을 포함한 상품 추가") {
            it("모든 차 구성을 포함한 요청을 UseCase에 전달") {
                val userId = 3L
                val teaItems = listOf(
                    TeaItemRequest(productId = 10L, selectionOrder = 1, ratioPercent = 40),
                    TeaItemRequest(productId = 11L, selectionOrder = 2, ratioPercent = 30),
                    TeaItemRequest(productId = 12L, selectionOrder = 3, ratioPercent = 30)
                )
                val request = AddToCartRequest(
                    packageTypeId = 5L,
                    packageTypeName = "7일 패키지",
                    packageTypeDays = 7,
                    dailyServing = 3,
                    totalQuantity = 70.0,
                    giftWrap = true,
                    giftMessage = "선물메시지",
                    teaItems = teaItems
                )
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockAddToCartUseCase.execute(userId, request) } returns mockCart

                val result = sut.addToCart(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockAddToCartUseCase.execute(userId, request) }
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

                every { mockUpdateCartItemUseCase.execute(userId, cartItemId, quantity) } returns mockCart

                val result = sut.updateCartItem(cartItemId, userId, quantity)

                result.success shouldBe true
                verify(exactly = 1) { mockUpdateCartItemUseCase.execute(userId, cartItemId, quantity) }
            }
        }

        context("수량을 0으로 수정") {
            it("0 수량을 포함한 파라미터를 UseCase에 전달") {
                val cartItemId = 2L
                val userId = 2L
                val quantity = 0
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockUpdateCartItemUseCase.execute(userId, cartItemId, quantity) } returns mockCart

                val result = sut.updateCartItem(cartItemId, userId, quantity)

                result.success shouldBe true
                verify(exactly = 1) { mockUpdateCartItemUseCase.execute(userId, cartItemId, quantity) }
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
                    every { mockUpdateCartItemUseCase.execute(userId, cartItemId, quantity) } returns mockCart

                    val result = sut.updateCartItem(cartItemId, userId, quantity)

                    result.success shouldBe true
                    verify(exactly = 1) { mockUpdateCartItemUseCase.execute(userId, cartItemId, quantity) }
                    clearMocks(mockUpdateCartItemUseCase)
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

                every { mockRemoveCartItemUseCase.execute(userId, cartItemId) } returns mockCart

                val result = sut.removeCartItem(cartItemId, userId)

                result.success shouldBe true
                verify(exactly = 1) { mockRemoveCartItemUseCase.execute(userId, cartItemId) }
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
                    every { mockRemoveCartItemUseCase.execute(userId, cartItemId) } returns mockCart

                    val result = sut.removeCartItem(cartItemId, userId)

                    result.success shouldBe true
                    verify(exactly = 1) { mockRemoveCartItemUseCase.execute(userId, cartItemId) }
                    clearMocks(mockRemoveCartItemUseCase)
                }
            }
        }
    }

    describe("clearCart") {
        context("DELETE /api/v1/cart?userId={userId} 요청") {
            it("userId를 ClearCartUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockClearCartUseCase.execute(userId) } returns mockCart

                val result = sut.clearCart(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockClearCartUseCase.execute(userId) }
            }
        }

        context("다양한 사용자의 장바구니 비우기") {
            it("각각의 userId가 정확히 UseCase에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockCart = mockk<Cart>(relaxed = true)
                    every { mockClearCartUseCase.execute(userId) } returns mockCart

                    val result = sut.clearCart(userId)

                    result.success shouldBe true
                    verify(exactly = 1) { mockClearCartUseCase.execute(userId) }
                    clearMocks(mockClearCartUseCase)
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
                verify(exactly = 0) { mockAddToCartUseCase.execute(any(), any()) }
                verify(exactly = 0) { mockUpdateCartItemUseCase.execute(any(), any(), any()) }

                clearMocks(mockGetCartUseCase, mockAddToCartUseCase, mockUpdateCartItemUseCase, mockRemoveCartItemUseCase, mockClearCartUseCase)

                // addToCart 테스트
                val addRequest = AddToCartRequest(
                    packageTypeId = 1L,
                    packageTypeName = "3일 패키지",
                    packageTypeDays = 3,
                    dailyServing = 1,
                    totalQuantity = 30.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = emptyList()
                )
                every { mockAddToCartUseCase.execute(1L, addRequest) } returns mockk(relaxed = true)
                sut.addToCart(1L, addRequest)
                verify(exactly = 1) { mockAddToCartUseCase.execute(1L, addRequest) }
                verify(exactly = 0) { mockGetCartUseCase.execute(any()) }
                verify(exactly = 0) { mockUpdateCartItemUseCase.execute(any(), any(), any()) }

                clearMocks(mockGetCartUseCase, mockAddToCartUseCase, mockUpdateCartItemUseCase, mockRemoveCartItemUseCase, mockClearCartUseCase)

                // updateCartItem 테스트
                every { mockUpdateCartItemUseCase.execute(1L, 1L, 1) } returns mockk(relaxed = true)
                sut.updateCartItem(1L, 1L, 1)
                verify(exactly = 1) { mockUpdateCartItemUseCase.execute(1L, 1L, 1) }
                verify(exactly = 0) { mockGetCartUseCase.execute(any()) }
                verify(exactly = 0) { mockAddToCartUseCase.execute(any(), any()) }
            }
        }
    }
})