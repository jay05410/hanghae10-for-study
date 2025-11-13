package io.hhplus.ecommerce.unit.order.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService

class GetOrderQueryUseCaseTest : DescribeSpec({

    val orderService = mockk<OrderService>()
    val getOrderQueryUseCase = GetOrderQueryUseCase(orderService)

    describe("GetOrderQueryUseCase") {

        beforeEach {
            clearAllMocks()
        }

        context("주문 ID로 조회 시") {
            it("should return order by id") {
                // Given
                val orderId = 1L

                val mockOrder = mockk<Order> {
                    every { id } returns orderId
                }

                every { orderService.getOrder(orderId) } returns mockOrder

                // When
                val result = getOrderQueryUseCase.getOrder(orderId)

                // Then
                result shouldBe mockOrder
                verify { orderService.getOrder(orderId) }
            }
        }

        context("사용자 ID로 주문 목록 조회 시") {
            it("should return orders by user id") {
                // Given
                val userId = 100L

                val mockOrders = listOf(
                    mockk<Order> { every { id } returns 1L },
                    mockk<Order> { every { id } returns 2L }
                )

                every { orderService.getOrdersByUser(userId) } returns mockOrders

                // When
                val result = getOrderQueryUseCase.getOrdersByUser(userId)

                // Then
                result shouldBe mockOrders
                verify { orderService.getOrdersByUser(userId) }
            }
        }

        context("빈 주문 목록 조회 시") {
            it("should return empty list when no orders") {
                // Given
                val userId = 999L
                val emptyList = emptyList<Order>()

                every { orderService.getOrdersByUser(userId) } returns emptyList

                // When
                val result = getOrderQueryUseCase.getOrdersByUser(userId)

                // Then
                result shouldBe emptyList
                verify { orderService.getOrdersByUser(userId) }
            }
        }

        context("서비스 호출 확인 시") {
            it("should delegate to order service correctly") {
                // Given
                val orderId = 555L
                val userId = 777L

                val mockOrder = mockk<Order>()
                val mockOrders = listOf(mockOrder)

                every { orderService.getOrder(orderId) } returns mockOrder
                every { orderService.getOrdersByUser(userId) } returns mockOrders

                // When
                getOrderQueryUseCase.getOrder(orderId)
                getOrderQueryUseCase.getOrdersByUser(userId)

                // Then
                verify(exactly = 1) { orderService.getOrder(orderId) }
                verify(exactly = 1) { orderService.getOrdersByUser(userId) }
            }
        }
    }
})