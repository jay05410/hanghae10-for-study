package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * RemoveCartItemUseCase 단위 테스트
 *
 * 책임: 장바구니 아이템 제거 비즈니스 흐름 검증
 * - 아이템 제거 로직의 서비스 위임 검증
 * - 파라미터 전달 및 결과 반환 검증
 *
 * 검증 목표:
 * 1. CartService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 다양한 사용자와 아이템 조합에 대한 처리가 올바른가?
 */
class RemoveCartItemUseCaseTest : DescribeSpec({
    val mockCartService = mockk<CartService>()
    val sut = RemoveCartItemUseCase(mockCartService)

    beforeEach {
        clearMocks(mockCartService)
    }

    describe("execute") {
        context("정상적인 아이템 제거") {
            it("CartService에 제거를 위임하고 업데이트된 장바구니를 반환") {
                val userId = 1L
                val cartItemId = 1L
                val expectedCart = mockk<Cart>()

                every { mockCartService.removeCartItem(userId, cartItemId) } returns expectedCart

                val result = sut.execute(userId, cartItemId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.removeCartItem(userId, cartItemId) }
            }
        }

        context("다른 사용자의 아이템 제거") {
            it("각각 다른 사용자와 아이템에 대해 정확한 파라미터 전달") {
                val userId = 2L
                val cartItemId = 5L
                val expectedCart = mockk<Cart>()

                every { mockCartService.removeCartItem(userId, cartItemId) } returns expectedCart

                val result = sut.execute(userId, cartItemId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.removeCartItem(userId, cartItemId) }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 서비스에 전달되는지 확인") {
                val testCases = listOf(
                    Pair(1L, 1L),
                    Pair(100L, 200L),
                    Pair(999L, 888L)
                )

                testCases.forEach { (userId, cartItemId) ->
                    val expectedCart = mockk<Cart>()
                    every { mockCartService.removeCartItem(userId, cartItemId) } returns expectedCart

                    val result = sut.execute(userId, cartItemId)

                    result shouldBe expectedCart
                    verify(exactly = 1) { mockCartService.removeCartItem(userId, cartItemId) }
                    clearMocks(mockCartService)
                }
            }
        }

        context("연속된 제거 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val cartItemId1 = 1L
                val cartItemId2 = 2L
                val expectedCart1 = mockk<Cart>()
                val expectedCart2 = mockk<Cart>()

                every { mockCartService.removeCartItem(userId, cartItemId1) } returns expectedCart1
                every { mockCartService.removeCartItem(userId, cartItemId2) } returns expectedCart2

                val result1 = sut.execute(userId, cartItemId1)
                val result2 = sut.execute(userId, cartItemId2)

                result1 shouldBe expectedCart1
                result2 shouldBe expectedCart2
                verify(exactly = 1) { mockCartService.removeCartItem(userId, cartItemId1) }
                verify(exactly = 1) { mockCartService.removeCartItem(userId, cartItemId2) }
            }
        }

        context("큰 ID 값 처리") {
            it("큰 ID 값도 정확히 전달") {
                val userId = 999999L
                val cartItemId = 888888L
                val expectedCart = mockk<Cart>()

                every { mockCartService.removeCartItem(userId, cartItemId) } returns expectedCart

                val result = sut.execute(userId, cartItemId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.removeCartItem(userId, cartItemId) }
            }
        }
    }
})