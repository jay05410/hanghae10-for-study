package io.hhplus.ecommerce.order.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService

class ConfirmOrderUseCaseTest : DescribeSpec({

    val orderService = mockk<OrderService>()
    val confirmOrderUseCase = ConfirmOrderUseCase(orderService)

    describe("ConfirmOrderUseCase") {

        beforeEach {
            clearAllMocks()
        }

        context("주문 확정 요청 시") {
            it("should confirm order successfully") {
                // Given
                val orderId = 1L
                val userId = 100L

                val mockOrder = mockk<Order> {
                    every { id } returns orderId
                }

                every { orderService.confirmOrder(orderId, userId) } returns mockOrder

                // When
                val result = confirmOrderUseCase.execute(orderId, userId)

                // Then
                result shouldBe mockOrder
                verify { orderService.confirmOrder(orderId, userId) }
            }
        }

        context("다른 사용자의 주문 확정 요청 시") {
            it("should delegate to order service") {
                // Given
                val orderId = 2L
                val userId = 200L

                val mockOrder = mockk<Order> {
                    every { id } returns orderId
                }

                every { orderService.confirmOrder(orderId, userId) } returns mockOrder

                // When
                val result = confirmOrderUseCase.execute(orderId, userId)

                // Then
                result shouldBe mockOrder
                verify(exactly = 1) { orderService.confirmOrder(orderId, userId) }
            }
        }

        context("주문 서비스 호출 시") {
            it("should call order service with correct parameters") {
                // Given
                val orderId = 999L
                val userId = 777L

                val mockOrder = mockk<Order>()
                every { orderService.confirmOrder(orderId, userId) } returns mockOrder

                // When
                confirmOrderUseCase.execute(orderId, userId)

                // Then
                verify { orderService.confirmOrder(orderId, userId) }
            }
        }
    }
})