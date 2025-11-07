package io.hhplus.ecommerce.order.controller

import io.hhplus.ecommerce.order.usecase.*
import io.hhplus.ecommerce.order.dto.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

/**
 * OrderController 단위 테스트
 *
 * 책임: 주문 관리 API 컨트롤러의 REST 엔드포인트 검증
 * - HTTP 요청/응답 처리 및 데이터 변환 검증
 * - UseCase와의 상호작용 및 위임 검증
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

    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(sut).build()
    val objectMapper = ObjectMapper()

    fun createMockOrder(
        id: Long = 1L,
        orderNumber: String = "ORD-001",
        userId: Long = 1L,
        status: OrderStatus = OrderStatus.PENDING
    ): Order = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.orderNumber } returns orderNumber
        every { this@mockk.userId } returns userId
        every { this@mockk.status } returns status
        every { totalAmount } returns 10000L
        every { finalAmount } returns 10000L
        every { createdAt } returns LocalDateTime.now()
    }

    beforeEach {
        clearMocks(
            mockCreateOrderUseCase,
            mockGetOrderQueryUseCase,
            mockConfirmOrderUseCase,
            mockCancelOrderUseCase
        )
    }

    describe("createOrder") {
        context("정상적인 주문 생성 요청") {
            it("UseCase를 호출하고 201과 주문 정보를 반환") {
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(productId = 1L, boxTypeId = 1L, quantity = 2)
                    )
                )
                val mockOrder = createMockOrder()

                every { mockCreateOrderUseCase.execute(request) } returns mockOrder

                mockMvc.perform(
                    post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))

                verify(exactly = 1) { mockCreateOrderUseCase.execute(request) }
            }
        }
    }

    describe("getOrder") {
        context("주문 ID로 조회") {
            it("UseCase를 호출하고 주문 정보를 반환") {
                val orderId = 1L
                val mockOrder = createMockOrder(id = orderId)

                every { mockGetOrderQueryUseCase.getOrder(orderId) } returns mockOrder

                mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))

                verify(exactly = 1) { mockGetOrderQueryUseCase.getOrder(orderId) }
            }
        }
    }

    describe("getOrders") {
        context("사용자 ID로 주문 목록 조회") {
            it("UseCase를 호출하고 주문 목록을 반환") {
                val userId = 1L
                val mockOrders = listOf(createMockOrder(userId = userId))

                every { mockGetOrderQueryUseCase.getOrdersByUser(userId) } returns mockOrders

                mockMvc.perform(get("/api/v1/orders").param("userId", userId.toString()))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))

                verify(exactly = 1) { mockGetOrderQueryUseCase.getOrdersByUser(userId) }
            }
        }
    }

    describe("confirmOrder") {
        context("주문 확정 요청") {
            it("UseCase를 호출하고 확정된 주문을 반환") {
                val orderId = 1L
                val request = OrderConfirmRequest(confirmedBy = 1L)
                val mockOrder = createMockOrder(id = orderId, status = OrderStatus.CONFIRMED)

                every { mockConfirmOrderUseCase.execute(orderId, request.confirmedBy) } returns mockOrder

                mockMvc.perform(
                    post("/api/v1/orders/{orderId}/confirm", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)

                verify(exactly = 1) { mockConfirmOrderUseCase.execute(orderId, request.confirmedBy) }
            }
        }
    }

    describe("cancelOrder") {
        context("주문 취소 요청") {
            it("UseCase를 호출하고 취소된 주문을 반환") {
                val orderId = 1L
                val request = OrderCancelRequest(cancelledBy = 1L, reason = "변심")
                val mockOrder = createMockOrder(id = orderId, status = OrderStatus.CANCELLED)

                every { mockCancelOrderUseCase.execute(orderId, request.cancelledBy, request.reason) } returns mockOrder

                mockMvc.perform(
                    post("/api/v1/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)

                verify(exactly = 1) { mockCancelOrderUseCase.execute(orderId, request.cancelledBy, request.reason) }
            }
        }
    }
})