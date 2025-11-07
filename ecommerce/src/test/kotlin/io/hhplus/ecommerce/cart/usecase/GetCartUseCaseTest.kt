package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetCartUseCase 단위 테스트
 *
 * 책임: 장바구니 조회 비즈니스 흐름 검증
 * - 사용자별 장바구니 조회 로직의 서비스 위임 검증
 * - 단순한 조회 작업의 파라미터 전달 검증
 *
 * 검증 목표:
 * 1. CartService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 존재하지 않는 장바구니에 대한 처리가 올바른가?
 */
class GetCartUseCaseTest : DescribeSpec({
    val mockCartService = mockk<CartService>()
    val sut = GetCartUseCase(mockCartService)

    beforeEach {
        clearMocks(mockCartService)
    }

    describe("execute") {
        context("장바구니가 존재하는 사용자") {
            it("CartService에 조회를 위임하고 장바구니를 반환") {
                val userId = 1L
                val expectedCart = mockk<Cart>()

                every { mockCartService.getCartByUser(userId) } returns expectedCart

                val result = sut.execute(userId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.getCartByUser(userId) }
            }
        }

        context("장바구니가 없는 사용자") {
            it("CartService에 조회를 위임하고 null을 반환") {
                val userId = 999L

                every { mockCartService.getCartByUser(userId) } returns null

                val result = sut.execute(userId)

                result shouldBe null
                verify(exactly = 1) { mockCartService.getCartByUser(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("각각의 사용자 ID를 정확히 서비스에 전달") {
                val userIds = listOf(1L, 100L, 999L)
                val mockCarts = userIds.map { mockk<Cart>() }

                userIds.forEachIndexed { index, userId ->
                    every { mockCartService.getCartByUser(userId) } returns mockCarts[index]

                    val result = sut.execute(userId)

                    result shouldBe mockCarts[index]
                    verify(exactly = 1) { mockCartService.getCartByUser(userId) }
                    clearMocks(mockCartService)
                }
            }
        }

        context("연속된 조회 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val mockCart1 = mockk<Cart>()
                val mockCart2 = mockk<Cart>()

                every { mockCartService.getCartByUser(userId) } returnsMany listOf(mockCart1, mockCart2)

                val result1 = sut.execute(userId)
                val result2 = sut.execute(userId)

                result1 shouldBe mockCart1
                result2 shouldBe mockCart2
                verify(exactly = 2) { mockCartService.getCartByUser(userId) }
            }
        }
    }
})