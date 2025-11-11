package io.hhplus.ecommerce.order.application

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import io.hhplus.ecommerce.order.domain.entity.OrderItemTea
import io.hhplus.ecommerce.order.domain.repository.OrderItemTeaRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest

class OrderItemTeaServiceTest : DescribeSpec({

    val orderItemTeaRepository = mockk<OrderItemTeaRepository>()
    val orderItemTeaService = OrderItemTeaService(orderItemTeaRepository)

    fun createMockOrderItemTea(
        id: Long = 1L,
        orderItemId: Long = 1L,
        productId: Long = 1L,
        quantity: Int = 5
    ): OrderItemTea = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.orderItemId } returns orderItemId
        every { this@mockk.productId } returns productId
        every { this@mockk.quantity } returns quantity
    }

    describe("OrderItemTeaService") {

        beforeEach {
            clearAllMocks()
        }

        context("주문 아이템 차 구성 저장 시") {
            it("should save order item teas successfully") {
                // Given
                val orderItemId = 1L
                val teaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 60),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 40)
                )

                val mockOrderItemTea1 = createMockOrderItemTea(1L, orderItemId, 1L, 3)
                val mockOrderItemTea2 = createMockOrderItemTea(2L, orderItemId, 2L, 2)

                mockkObject(OrderItemTea.Companion)
                every { OrderItemTea.create(
                    orderItemId = orderItemId,
                    productId = 1L,
                    productName = "Product 1",
                    categoryName = "Category",
                    selectionOrder = 1,
                    ratioPercent = 60,
                    unitPrice = 1000
                ) } returns mockOrderItemTea1
                every { OrderItemTea.create(
                    orderItemId = orderItemId,
                    productId = 2L,
                    productName = "Product 2",
                    categoryName = "Category",
                    selectionOrder = 2,
                    ratioPercent = 40,
                    unitPrice = 1000
                ) } returns mockOrderItemTea2
                every { orderItemTeaRepository.save(mockOrderItemTea1) } returns mockOrderItemTea1
                every { orderItemTeaRepository.save(mockOrderItemTea2) } returns mockOrderItemTea2

                // When
                val result = orderItemTeaService.saveOrderItemTeas(orderItemId, teaItems)

                // Then
                result.size shouldBe 2
                result[0] shouldBe mockOrderItemTea1
                result[1] shouldBe mockOrderItemTea2
                verify { orderItemTeaRepository.save(mockOrderItemTea1) }
                verify { orderItemTeaRepository.save(mockOrderItemTea2) }
            }
        }

        context("주문 아이템 차 구성 조회 시") {
            it("should return order item teas by order item id") {
                // Given
                val orderItemId = 1L
                val mockTeas = listOf(
                    createMockOrderItemTea(1L, orderItemId, 1L, 3),
                    createMockOrderItemTea(2L, orderItemId, 2L, 2)
                )

                every { orderItemTeaRepository.findByOrderItemId(orderItemId) } returns mockTeas

                // When
                val result = orderItemTeaService.getOrderItemTeas(orderItemId)

                // Then
                result shouldBe mockTeas
                verify { orderItemTeaRepository.findByOrderItemId(orderItemId) }
            }
        }

        context("주문 아이템 차 구성 삭제 시") {
            it("should delete order item teas by order item id") {
                // Given
                val orderItemId = 1L

                every { orderItemTeaRepository.deleteByOrderItemId(orderItemId) } just Runs

                // When
                orderItemTeaService.deleteOrderItemTeas(orderItemId)

                // Then
                verify { orderItemTeaRepository.deleteByOrderItemId(orderItemId) }
            }
        }

        context("차 구성 유효성 검증 시") {
            it("should validate tea items successfully") {
                // Given
                val validTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 60),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 40)
                )

                // When & Then
                orderItemTeaService.validateTeaItemsForOrder(validTeaItems)
            }

            it("should throw exception when tea items is empty") {
                // Given
                val emptyTeaItems = emptyList<TeaItemRequest>()

                // When & Then
                shouldThrow<IllegalArgumentException> {
                    orderItemTeaService.validateTeaItemsForOrder(emptyTeaItems)
                }
            }

            it("should throw exception when total quantity is zero") {
                // Given
                val zeroQuantityTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 0),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 0)
                )

                // When & Then
                shouldThrow<IllegalArgumentException> {
                    orderItemTeaService.validateTeaItemsForOrder(zeroQuantityTeaItems)
                }
            }

            it("should throw exception when duplicate products exist") {
                // Given
                val duplicateTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 60),
                    TeaItemRequest(productId = 1L, selectionOrder = 2, ratioPercent = 40)
                )

                // When & Then
                shouldThrow<IllegalArgumentException> {
                    orderItemTeaService.validateTeaItemsForOrder(duplicateTeaItems)
                }
            }
        }

        context("차 총 비율 계산 시") {
            it("should calculate total ratio correctly") {
                // Given
                val teaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 50),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 33),
                    TeaItemRequest(productId = 3L, selectionOrder = 3, ratioPercent = 17)
                )

                // When
                val result = orderItemTeaService.calculateTeaTotalRatio(teaItems)

                // Then
                result shouldBe 100
            }

            it("should return zero for empty list") {
                // Given
                val emptyTeaItems = emptyList<TeaItemRequest>()

                // When
                val result = orderItemTeaService.calculateTeaTotalRatio(emptyTeaItems)

                // Then
                result shouldBe 0
            }
        }

        context("차 상품 ID 목록 조회 시") {
            it("should return tea product ids") {
                // Given
                val orderItemId = 1L
                val mockTeas = listOf(
                    createMockOrderItemTea(1L, orderItemId, 10L, 3),
                    createMockOrderItemTea(2L, orderItemId, 20L, 2)
                )

                every { orderItemTeaRepository.findByOrderItemId(orderItemId) } returns mockTeas

                // When
                val result = orderItemTeaService.getTeaProductIds(orderItemId)

                // Then
                result shouldBe listOf(10L, 20L)
                verify { orderItemTeaRepository.findByOrderItemId(orderItemId) }
            }
        }
    }
})