package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ClearCartUseCase 단위 테스트
 *
 * 책임: 장바구니 비우기 비즈니스 흐름 검증
 * - 장바구니 전체 비우기 로직의 서비스 위임 검증
 * - 파라미터 전달 및 결과 반환 검증
 *
 * 검증 목표:
 * 1. CartService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 다양한 사용자에 대한 처리가 올바른가?
 */
class ClearCartUseCaseTest : DescribeSpec({
    val mockCartService = mockk<CartService>()
    val sut = ClearCartUseCase(mockCartService)

    beforeEach {
        clearMocks(mockCartService)
    }

    describe("execute") {
        context("정상적인 장바구니 비우기") {
            it("CartService에 비우기를 위임하고 비워진 장바구니를 반환") {
                val userId = 1L
                val expectedCart = mockk<Cart>()

                every { mockCartService.clearCart(userId) } returns expectedCart

                val result = sut.execute(userId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.clearCart(userId) }
            }
        }

        context("다른 사용자의 장바구니 비우기") {
            it("각각 다른 사용자에 대해 정확한 파라미터 전달") {
                val userId = 2L
                val expectedCart = mockk<Cart>()

                every { mockCartService.clearCart(userId) } returns expectedCart

                val result = sut.execute(userId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.clearCart(userId) }
            }
        }

        context("다양한 사용자 ID") {
            it("모든 사용자 ID가 정확히 서비스에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L, 1000000L)

                userIds.forEach { userId ->
                    val expectedCart = mockk<Cart>()
                    every { mockCartService.clearCart(userId) } returns expectedCart

                    val result = sut.execute(userId)

                    result shouldBe expectedCart
                    verify(exactly = 1) { mockCartService.clearCart(userId) }
                    clearMocks(mockCartService)
                }
            }
        }

        context("연속된 비우기 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val expectedCart1 = mockk<Cart>()
                val expectedCart2 = mockk<Cart>()

                every { mockCartService.clearCart(userId) } returnsMany listOf(expectedCart1, expectedCart2)

                val result1 = sut.execute(userId)
                val result2 = sut.execute(userId)

                result1 shouldBe expectedCart1
                result2 shouldBe expectedCart2
                verify(exactly = 2) { mockCartService.clearCart(userId) }
            }
        }

        context("여러 사용자의 동시 비우기") {
            it("각 사용자별로 독립적으로 처리") {
                val userId1 = 1L
                val userId2 = 2L
                val expectedCart1 = mockk<Cart>()
                val expectedCart2 = mockk<Cart>()

                every { mockCartService.clearCart(userId1) } returns expectedCart1
                every { mockCartService.clearCart(userId2) } returns expectedCart2

                val result1 = sut.execute(userId1)
                val result2 = sut.execute(userId2)

                result1 shouldBe expectedCart1
                result2 shouldBe expectedCart2
                verify(exactly = 1) { mockCartService.clearCart(userId1) }
                verify(exactly = 1) { mockCartService.clearCart(userId2) }
            }
        }
    }
})