package io.hhplus.ecommerce.unit.order.usecase

import io.hhplus.ecommerce.order.usecase.GetOrderQueryUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
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

        context("주문과 아이템을 함께 조회 시") {
            it("should return order with items") {
                // Given
                val orderId = 1L
                val mockOrder = mockk<Order> {
                    every { id } returns orderId
                }
                val mockOrderItems = listOf(
                    mockk<OrderItem> { every { id } returns 1L },
                    mockk<OrderItem> { every { id } returns 2L }
                )

                every { orderService.getOrderWithItems(orderId) } returns Pair(mockOrder, mockOrderItems)

                // When
                val result = getOrderQueryUseCase.getOrderWithItems(orderId)

                // Then
                result shouldBe Pair(mockOrder, mockOrderItems)
                verify { orderService.getOrderWithItems(orderId) }
            }

            it("should return null when order not found") {
                // Given
                val orderId = 999L
                every { orderService.getOrderWithItems(orderId) } returns null

                // When
                val result = getOrderQueryUseCase.getOrderWithItems(orderId)

                // Then
                result shouldBe null
                verify { orderService.getOrderWithItems(orderId) }
            }
        }

        context("사용자의 주문과 아이템을 함께 조회 시") {
            it("should return orders with items grouped by order") {
                // Given
                val userId = 100L
                val order1 = mockk<Order> {
                    every { id } returns 1L
                }
                val order2 = mockk<Order> {
                    every { id } returns 2L
                }

                val orderItem1 = mockk<OrderItem> {
                    every { id } returns 1L
                    every { orderId } returns 1L
                }
                val orderItem2 = mockk<OrderItem> {
                    every { id } returns 2L
                    every { orderId } returns 2L
                }

                val expectedMap = mapOf(
                    order1 to listOf(orderItem1),
                    order2 to listOf(orderItem2)
                )

                every { orderService.getOrdersWithItemsByUser(userId) } returns expectedMap

                // When
                val result = getOrderQueryUseCase.getOrdersWithItemsByUser(userId)

                // Then
                result.size shouldBe 2
                result[order1] shouldBe listOf(orderItem1)
                result[order2] shouldBe listOf(orderItem2)
                verify { orderService.getOrdersWithItemsByUser(userId) }
            }

            it("should return empty map when no orders") {
                // Given
                val userId = 999L
                every { orderService.getOrdersWithItemsByUser(userId) } returns emptyMap()

                // When
                val result = getOrderQueryUseCase.getOrdersWithItemsByUser(userId)

                // Then
                result shouldBe emptyMap()
                verify { orderService.getOrdersWithItemsByUser(userId) }
            }
        }
    }
})