package io.hhplus.ecommerce.order.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService

class CancelOrderUseCaseTest : DescribeSpec({

    val orderService = mockk<OrderService>()
    val cancelOrderUseCase = CancelOrderUseCase(orderService)

    describe("CancelOrderUseCase") {

        beforeEach {
            clearAllMocks()
        }

        context("주문 취소 요청 시") {
            it("should cancel order successfully") {
                // Given
                val orderId = 1L
                val cancelledBy = 100L
                val reason = "고객 요청"

                val mockOrder = mockk<Order> {
                    every { id } returns orderId
                }

                every { orderService.cancelOrder(orderId, cancelledBy, reason) } returns mockOrder

                // When
                val result = cancelOrderUseCase.execute(orderId, cancelledBy, reason)

                // Then
                result shouldBe mockOrder
                verify { orderService.cancelOrder(orderId, cancelledBy, reason) }
            }
        }

        context("취소 사유 없이 주문 취소 요청 시") {
            it("should cancel order without reason") {
                // Given
                val orderId = 2L
                val cancelledBy = 200L
                val reason = null

                val mockOrder = mockk<Order> {
                    every { id } returns orderId
                }

                every { orderService.cancelOrder(orderId, cancelledBy, reason) } returns mockOrder

                // When
                val result = cancelOrderUseCase.execute(orderId, cancelledBy, reason)

                // Then
                result shouldBe mockOrder
                verify { orderService.cancelOrder(orderId, cancelledBy, reason) }
            }
        }

        context("주문 서비스 호출 시") {
            it("should delegate to order service with correct parameters") {
                // Given
                val orderId = 999L
                val cancelledBy = 777L
                val reason = "배송 지연"

                val mockOrder = mockk<Order>()
                every { orderService.cancelOrder(orderId, cancelledBy, reason) } returns mockOrder

                // When
                cancelOrderUseCase.execute(orderId, cancelledBy, reason)

                // Then
                verify(exactly = 1) { orderService.cancelOrder(orderId, cancelledBy, reason) }
            }
        }
    }
})