package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * UpdateCartItemUseCase 단위 테스트
 *
 * 책임: 장바구니 아이템 수량 수정 비즈니스 흐름 검증
 * - 아이템 수량 수정 로직의 서비스 위임 검증
 * - 파라미터 전달 및 결과 반환 검증
 *
 * 검증 목표:
 * 1. CartService에 올바른 파라미터가 전달되는가?
 * 2. updatedBy 파라미터가 userId와 동일하게 설정되는가?
 * 3. 서비스 결과가 그대로 반환되는가?
 * 4. 다양한 수량 값에 대한 처리가 올바른가?
 */
class UpdateCartItemUseCaseTest : DescribeSpec({
    val mockCartService = mockk<CartService>()
    val sut = UpdateCartItemUseCase(mockCartService)

    beforeEach {
        clearMocks(mockCartService)
    }

    describe("execute") {
        context("정상적인 수량 수정") {
            it("CartService에 수정을 위임하고 업데이트된 장바구니를 반환") {
                val userId = 1L
                val cartItemId = 1L
                val quantity = 5
                val expectedCart = mockk<Cart>()

                every { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) } returns expectedCart

                val result = sut.execute(userId, cartItemId, quantity)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) }
            }
        }

        context("수량을 0으로 수정") {
            it("CartService에 0 수량 수정을 위임하여 아이템 제거 처리") {
                val userId = 2L
                val cartItemId = 2L
                val quantity = 0
                val expectedCart = mockk<Cart>()

                every { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) } returns expectedCart

                val result = sut.execute(userId, cartItemId, quantity)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) }
            }
        }

        context("음수 수량으로 수정") {
            it("CartService에 음수 수량을 전달하여 아이템 제거 처리") {
                val userId = 3L
                val cartItemId = 3L
                val quantity = -1
                val expectedCart = mockk<Cart>()

                every { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) } returns expectedCart

                val result = sut.execute(userId, cartItemId, quantity)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 서비스에 전달되는지 확인") {
                val testCases = listOf(
                    Triple(1L, 1L, 1),
                    Triple(100L, 200L, 10),
                    Triple(999L, 888L, 999)
                )

                testCases.forEach { (userId, cartItemId, quantity) ->
                    val expectedCart = mockk<Cart>()
                    every { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) } returns expectedCart

                    val result = sut.execute(userId, cartItemId, quantity)

                    result shouldBe expectedCart
                    verify(exactly = 1) { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) }
                    clearMocks(mockCartService)
                }
            }
        }

        context("updatedBy 파라미터 검증") {
            it("updatedBy가 userId와 동일하게 설정되는지 확인") {
                val userId = 42L
                val cartItemId = 1L
                val quantity = 3
                val expectedCart = mockk<Cart>()

                every { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) } returns expectedCart

                sut.execute(userId, cartItemId, quantity)

                verify(exactly = 1) { mockCartService.updateCartItem(
                    userId = userId,
                    cartItemId = cartItemId,
                    quantity = quantity,
                    updatedBy = userId  // updatedBy가 userId와 동일해야 함
                ) }
            }
        }

        context("큰 수량 값으로 수정") {
            it("큰 수량 값도 정확히 전달") {
                val userId = 1L
                val cartItemId = 1L
                val quantity = 1000
                val expectedCart = mockk<Cart>()

                every { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) } returns expectedCart

                val result = sut.execute(userId, cartItemId, quantity)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartService.updateCartItem(userId, cartItemId, quantity, userId) }
            }
        }
    }
})