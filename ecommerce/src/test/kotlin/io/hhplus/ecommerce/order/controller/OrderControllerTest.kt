package io.hhplus.ecommerce.order.controller

import io.hhplus.ecommerce.order.usecase.*
import io.hhplus.ecommerce.order.dto.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * OrderController 단위 테스트
 *
 * 책임: 주문 컨트롤러의 HTTP 요청/응답 처리 검증
 * - REST API 엔드포인트 동작 검증
 * - UseCase와의 상호작용 검증
 */
class OrderControllerTest : DescribeSpec({
    val mockCreateOrderUseCase = mockk<CreateOrderUseCase>()
    val mockGetOrderQueryUseCase = mockk<GetOrderQueryUseCase>()
    val mockConfirmOrderUseCase = mockk<ConfirmOrderUseCase>()
    val mockCancelOrderUseCase = mockk<CancelOrderUseCase>()

    val sut = OrderController(
        createOrderUseCase = mockCreateOrderUseCase,
        getOrderQueryUseCase = mockGetOrderQueryUseCase,
        confirmOrderUseCase = mockConfirmOrderUseCase,
        cancelOrderUseCase = mockCancelOrderUseCase
    )

    fun createMockOrder(
        id: Long = 1L,
        orderNumber: String = "ORD-20241107-001",
        userId: Long = 1L,
        totalAmount: Long = 10000L,
        discountAmount: Long = 0L,
        status: OrderStatus = OrderStatus.PENDING
    ): Order = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.orderNumber } returns orderNumber
        every { this@mockk.userId } returns userId
        every { this@mockk.totalAmount } returns totalAmount
        every { this@mockk.discountAmount } returns discountAmount
        every { this@mockk.finalAmount } returns (totalAmount - discountAmount)
        every { this@mockk.status } returns status
        every { this@mockk.usedCouponId } returns null
        every { items } returns emptyList()
        every { isActive } returns true
        every { createdAt } returns LocalDateTime.now()
        every { updatedAt } returns LocalDateTime.now()
    }

    beforeEach {
        clearMocks(mockCreateOrderUseCase, mockGetOrderQueryUseCase, mockConfirmOrderUseCase, mockCancelOrderUseCase)
    }

    describe("createOrder") {
        context("POST /api/v1/orders 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(
                            productId = 1L,
                            boxTypeId = 1L,
                            quantity = 2
                        )
                    )
                )
                val mockOrder = createMockOrder(userId = 1L)

                every { mockCreateOrderUseCase.execute(request) } returns mockOrder

                val result = sut.createOrder(request)

                result shouldBe ApiResponse.success(mockOrder)
                verify(exactly = 1) { mockCreateOrderUseCase.execute(request) }
            }
        }
    }

    describe("getOrder") {
        context("GET /api/v1/orders/{orderId} 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val orderId = 1L
                val mockOrder = createMockOrder(id = orderId)

                every { mockGetOrderQueryUseCase.getOrder(orderId) } returns mockOrder

                val result = sut.getOrder(orderId)

                result shouldBe ApiResponse.success(mockOrder)
                verify(exactly = 1) { mockGetOrderQueryUseCase.getOrder(orderId) }
            }
        }

        context("존재하지 않는 주문 조회") {
            it("UseCase에서 null 반환 시 처리") {
                val orderId = 999L

                every { mockGetOrderQueryUseCase.getOrder(orderId) } returns null

                val result = sut.getOrder(orderId)

                result shouldBe ApiResponse.success(null)
                verify(exactly = 1) { mockGetOrderQueryUseCase.getOrder(orderId) }
            }
        }
    }

    describe("getOrders") {
        context("GET /api/v1/orders?userId=x 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val userId = 1L
                val mockOrders = listOf(
                    createMockOrder(id = 1L, userId = userId),
                    createMockOrder(id = 2L, userId = userId)
                )

                every { mockGetOrderQueryUseCase.getOrdersByUser(userId) } returns mockOrders

                val result = sut.getOrders(userId)

                result shouldBe ApiResponse.success(mockOrders)
                verify(exactly = 1) { mockGetOrderQueryUseCase.getOrdersByUser(userId) }
            }
        }

        context("사용자의 주문이 없는 경우") {
            it("빈 목록을 ApiResponse로 감싸서 반환") {
                val userId = 999L

                every { mockGetOrderQueryUseCase.getOrdersByUser(userId) } returns emptyList()

                val result = sut.getOrders(userId)

                result shouldBe ApiResponse.success(emptyList<Order>())
                verify(exactly = 1) { mockGetOrderQueryUseCase.getOrdersByUser(userId) }
            }
        }
    }
})