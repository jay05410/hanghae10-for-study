package io.hhplus.ecommerce.order.application

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.entity.OrderItemTea
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.util.IdPrefix
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.*
import java.time.LocalDateTime

/**
 * OrderService 단위 테스트
 *
 * 책임: 주문 서비스의 핵심 비즈니스 로직 검증
 * - 주문 생성, 수정, 조회, 확정, 취소 기능 검증
 * - Repository와의 상호작용 및 트랜잭션 처리 검증
 */
class OrderServiceTest : DescribeSpec({
    val mockOrderRepository = mockk<OrderRepository>()
    val mockOrderItemTeaService = mockk<OrderItemTeaService>()
    val mockSnowflakeGenerator = mockk<SnowflakeGenerator>()
    val mockProductStatisticsService = mockk<ProductStatisticsService>()
    val mockOutboxEventService = mockk<OutboxEventService>()
    val mockObjectMapper = mockk<ObjectMapper>()

    val sut = OrderService(
        orderRepository = mockOrderRepository,
        orderItemTeaService = mockOrderItemTeaService,
        snowflakeGenerator = mockSnowflakeGenerator,
        productStatisticsService = mockProductStatisticsService,
        outboxEventService = mockOutboxEventService,
        objectMapper = mockObjectMapper
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
        every { createdBy } returns userId
        every { updatedBy } returns userId
        every { deletedAt } returns null
        every { confirm(any()) } returns Unit
        every { cancel(any()) } returns Unit
        every { addItem(any(), any(), any(), any()) } returns mockk<OrderItem>(relaxed = true)
    }

    fun createMockOrderItem(
        id: Long = 1L,
        productId: Long = 1L,
        quantity: Int = 2
    ): OrderItem = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.productId } returns productId
        every { this@mockk.quantity } returns quantity
    }

    beforeEach {
        clearMocks(
            mockOrderRepository,
            mockOrderItemTeaService,
            mockSnowflakeGenerator,
            mockProductStatisticsService,
            mockOutboxEventService,
            mockObjectMapper
        )
    }

    describe("getOrder") {
        context("존재하는 주문 조회") {
            it("Repository에서 주문을 조회하여 반환") {
                val orderId = 1L
                val mockOrder = createMockOrder(id = orderId)

                every { mockOrderRepository.findById(orderId) } returns mockOrder

                val result = sut.getOrder(orderId)

                result shouldBe mockOrder
                verify(exactly = 1) { mockOrderRepository.findById(orderId) }
            }
        }

        context("존재하지 않는 주문 조회") {
            it("null을 반환") {
                val orderId = 999L

                every { mockOrderRepository.findById(orderId) } returns null

                val result = sut.getOrder(orderId)

                result shouldBe null
                verify(exactly = 1) { mockOrderRepository.findById(orderId) }
            }
        }
    }

    describe("getOrdersByUser") {
        context("사용자의 주문이 있는 경우") {
            it("사용자의 활성 주문 목록을 반환") {
                val userId = 1L
                val mockOrders = listOf(
                    createMockOrder(id = 1L, userId = userId),
                    createMockOrder(id = 2L, userId = userId)
                )

                every { mockOrderRepository.findByUserIdAndIsActive(userId, true) } returns mockOrders

                val result = sut.getOrdersByUser(userId)

                result shouldHaveSize 2
                result shouldBe mockOrders
                verify(exactly = 1) { mockOrderRepository.findByUserIdAndIsActive(userId, true) }
            }
        }

        context("사용자의 주문이 없는 경우") {
            it("빈 목록을 반환") {
                val userId = 2L

                every { mockOrderRepository.findByUserIdAndIsActive(userId, true) } returns emptyList()

                val result = sut.getOrdersByUser(userId)

                result.shouldBeEmpty()
                verify(exactly = 1) { mockOrderRepository.findByUserIdAndIsActive(userId, true) }
            }
        }
    }

    describe("updateOrder") {
        context("주문 정보 업데이트") {
            it("Repository에 주문을 저장하고 반환") {
                val mockOrder = createMockOrder()

                every { mockOrderRepository.save(mockOrder) } returns mockOrder

                val result = sut.updateOrder(mockOrder)

                result shouldBe mockOrder
                verify(exactly = 1) { mockOrderRepository.save(mockOrder) }
            }
        }
    }

    describe("confirmOrder") {
        context("정상적인 주문 확정") {
            it("주문을 확정하고 저장") {
                val orderId = 1L
                val confirmedBy = 1L
                val mockOrder = createMockOrder(id = orderId, status = OrderStatus.PENDING)
                val mockOrderItems = listOf(
                    createMockOrderItem(id = 1L, productId = 1L, quantity = 2),
                    createMockOrderItem(id = 2L, productId = 2L, quantity = 1)
                )

                every { mockOrderRepository.findById(orderId) } returns mockOrder
                every { mockOrder.items } returns mockOrderItems
                every { mockProductStatisticsService.incrementSalesCount(any(), any(), any()) } returns mockk()
                every { mockOrderRepository.save(mockOrder) } returns mockOrder

                val result = sut.confirmOrder(orderId, confirmedBy)

                result shouldBe mockOrder
                verify(exactly = 1) { mockOrderRepository.findById(orderId) }
                verify(exactly = 1) { mockOrder.confirm(confirmedBy) }
                verify(exactly = 1) { mockOrderRepository.save(mockOrder) }
            }
        }

        context("존재하지 않는 주문 확정") {
            it("IllegalArgumentException을 발생") {
                val orderId = 999L
                val confirmedBy = 1L

                every { mockOrderRepository.findById(orderId) } returns null

                shouldThrow<IllegalArgumentException> {
                    sut.confirmOrder(orderId, confirmedBy)
                }

                verify(exactly = 1) { mockOrderRepository.findById(orderId) }
                verify(exactly = 0) { mockOrderRepository.save(any()) }
            }
        }
    }

    describe("cancelOrder") {
        context("정상적인 주문 취소") {
            it("주문을 취소하고 저장") {
                val orderId = 1L
                val cancelledBy = 1L
                val reason = "변심"
                val mockOrder = createMockOrder(id = orderId, status = OrderStatus.PENDING)
                val mockOrderItems = listOf(
                    createMockOrderItem(id = 1L),
                    createMockOrderItem(id = 2L)
                )

                every { mockOrderRepository.findById(orderId) } returns mockOrder
                every { mockOrder.items } returns mockOrderItems
                every { mockOrderItemTeaService.deleteOrderItemTeas(any()) } returns mockk()
                every { mockObjectMapper.writeValueAsString(any()) } returns "{\"orderId\":1}"
                every { mockOutboxEventService.publishEvent(any(), any(), any(), any(), any()) } returns mockk()
                every { mockOrderRepository.save(mockOrder) } returns mockOrder

                val result = sut.cancelOrder(orderId, cancelledBy, reason)

                result shouldBe mockOrder
                verify(exactly = 1) { mockOrderRepository.findById(orderId) }
                verify(exactly = 1) { mockOrder.cancel(cancelledBy) }
                verify(exactly = 1) { mockOrderRepository.save(mockOrder) }
            }
        }

        context("존재하지 않는 주문 취소") {
            it("IllegalArgumentException을 발생") {
                val orderId = 999L
                val cancelledBy = 1L

                every { mockOrderRepository.findById(orderId) } returns null

                shouldThrow<IllegalArgumentException> {
                    sut.cancelOrder(orderId, cancelledBy, null)
                }

                verify(exactly = 1) { mockOrderRepository.findById(orderId) }
                verify(exactly = 0) { mockOrderRepository.save(any()) }
            }
        }
    }

    describe("getOrderItemTeas") {
        context("주문 아이템의 차 구성 조회") {
            it("OrderItemTeaService를 호출하여 차 구성을 반환") {
                val orderItemId = 1L
                val mockOrderItemTeas = listOf(
                    mockk<OrderItemTea>(relaxed = true),
                    mockk<OrderItemTea>(relaxed = true)
                )

                every { mockOrderItemTeaService.getOrderItemTeas(orderItemId) } returns mockOrderItemTeas

                val result = sut.getOrderItemTeas(orderItemId)

                result shouldHaveSize 2
                result shouldBe mockOrderItemTeas
                verify(exactly = 1) { mockOrderItemTeaService.getOrderItemTeas(orderItemId) }
            }
        }

        context("차 구성이 없는 주문 아이템") {
            it("빈 목록을 반환") {
                val orderItemId = 2L

                every { mockOrderItemTeaService.getOrderItemTeas(orderItemId) } returns emptyList()

                val result = sut.getOrderItemTeas(orderItemId)

                result.shouldBeEmpty()
                verify(exactly = 1) { mockOrderItemTeaService.getOrderItemTeas(orderItemId) }
            }
        }
    }
})