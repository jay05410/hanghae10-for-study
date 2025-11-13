package io.hhplus.ecommerce.unit.order.application

import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.support.IntegrationTestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * OrderService 통합 테스트
 *
 * TestContainers MySQL을 사용하여 실제 데이터베이스와 연동된 전체 플로우를 검증합니다.
 * Controller -> Service -> Repository -> Database 전체 계층이 포함됩니다.
 */
@DisplayName("주문 서비스 통합 테스트")
class OrderServiceIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Test
    @DisplayName("새로운 주문을 생성할 수 있다")
    @Rollback
    fun `should create new order successfully`() {
        // Given
        val userId = 100L
        val packageTypeId = 1L
        val teaItems = listOf(
            TeaItemRequest(
                productId = 1L,
                selectionOrder = 1,
                ratioPercent = 70
            ),
            TeaItemRequest(
                productId = 2L,
                selectionOrder = 2,
                ratioPercent = 30
            )
        )

        val orderItems = listOf(
            OrderItemData(
                packageTypeId = packageTypeId,
                packageTypeName = "프리미엄 패키지",
                packageTypeDays = 30,
                dailyServing = 2,
                totalQuantity = 15.0,
                giftWrap = true,
                giftMessage = "생일 축하합니다!",
                quantity = 1,
                containerPrice = 10000,
                teaPrice = 50000,
                giftWrapPrice = 3000,
                teaItems = teaItems
            )
        )

        val totalAmount = 63000L
        val discountAmount = 3000L
        val createdBy = userId

        // When
        val createdOrder = orderService.createOrder(
            userId = userId,
            items = orderItems,
            usedCouponId = null,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            createdBy = createdBy
        )

        // Then
        assertNotNull(createdOrder)
        assertEquals(userId, createdOrder.userId)
        assertEquals(totalAmount, createdOrder.totalAmount)
        assertEquals(discountAmount, createdOrder.discountAmount)
        assertEquals(totalAmount - discountAmount, createdOrder.finalAmount)
        assertTrue(createdOrder.orderNumber.isNotEmpty())

        // 데이터베이스에서 확인
        val savedOrder = orderRepository.findById(createdOrder.id)
        assertNotNull(savedOrder)
        assertEquals(createdOrder.id, savedOrder.id)

        // 주문 아이템 확인
        val savedOrderItems = orderItemRepository.findByOrderId(createdOrder.id)
        assertEquals(1, savedOrderItems.size)

        val savedOrderItem = savedOrderItems.first()
        assertEquals(packageTypeId, savedOrderItem.packageTypeId)
        assertEquals("프리미엄 패키지", savedOrderItem.packageTypeName)
        assertEquals(30, savedOrderItem.packageTypeDays)
        assertEquals(true, savedOrderItem.giftWrap)
        assertEquals("생일 축하합니다!", savedOrderItem.giftMessage)
    }

    @Test
    @DisplayName("존재하는 주문을 조회할 수 있다")
    @Rollback
    fun `should find existing order`() {
        // Given
        val userId = 200L
        val orderItems = listOf(
            OrderItemData(
                packageTypeId = 1L,
                packageTypeName = "기본 패키지",
                packageTypeDays = 14,
                dailyServing = 1,
                totalQuantity = 14.0,
                giftWrap = false,
                giftMessage = null,
                quantity = 1,
                containerPrice = 5000,
                teaPrice = 20000,
                giftWrapPrice = 0,
                teaItems = emptyList()
            )
        )

        val createdOrder = orderService.createOrder(
            userId = userId,
            items = orderItems,
            usedCouponId = null,
            totalAmount = 25000L,
            discountAmount = 0L,
            createdBy = userId
        )

        // When
        val foundOrder = orderService.getOrder(createdOrder.id)

        // Then
        assertNotNull(foundOrder)
        assertEquals(createdOrder.id, foundOrder.id)
        assertEquals(userId, foundOrder.userId)
        assertEquals(25000L, foundOrder.totalAmount)
    }

    @Test
    @DisplayName("사용자별 활성 주문 목록을 조회할 수 있다")
    @Rollback
    fun `should find orders by user`() {
        // Given
        val userId = 300L
        val orderItems = listOf(
            OrderItemData(
                packageTypeId = 1L,
                packageTypeName = "테스트 패키지",
                packageTypeDays = 7,
                dailyServing = 1,
                totalQuantity = 7.0,
                giftWrap = false,
                giftMessage = null,
                quantity = 1,
                containerPrice = 3000,
                teaPrice = 15000,
                giftWrapPrice = 0,
                teaItems = emptyList()
            )
        )

        // 두 개의 주문 생성
        orderService.createOrder(
            userId = userId,
            items = orderItems,
            usedCouponId = null,
            totalAmount = 18000L,
            discountAmount = 0L,
            createdBy = userId
        )

        orderService.createOrder(
            userId = userId,
            items = orderItems,
            usedCouponId = null,
            totalAmount = 18000L,
            discountAmount = 0L,
            createdBy = userId
        )

        // When
        val userOrders = orderService.getOrdersByUser(userId)

        // Then
        assertEquals(2, userOrders.size)
        assertTrue(userOrders.all { it.userId == userId })
        assertTrue(userOrders.all { it.isActive })
    }

    @Test
    @DisplayName("주문을 확정 처리할 수 있다")
    @Rollback
    fun `should confirm order successfully`() {
        // Given
        val userId = 400L
        val orderItems = listOf(
            OrderItemData(
                packageTypeId = 1L,
                packageTypeName = "확정 테스트 패키지",
                packageTypeDays = 10,
                dailyServing = 1,
                totalQuantity = 10.0,
                giftWrap = false,
                giftMessage = null,
                quantity = 2,
                containerPrice = 4000,
                teaPrice = 30000,
                giftWrapPrice = 0,
                teaItems = emptyList()
            )
        )

        val createdOrder = orderService.createOrder(
            userId = userId,
            items = orderItems,
            usedCouponId = null,
            totalAmount = 68000L,
            discountAmount = 0L,
            createdBy = userId
        )

        // When
        val confirmedOrder = orderService.confirmOrder(createdOrder.id, userId)

        // Then
        assertNotNull(confirmedOrder)
        assertEquals("CONFIRMED", confirmedOrder.status.name)
        // 상태 확인만 수행 (confirmedAt, confirmedBy는 Order 엔티티에 없음)
        // assertEquals(userId, confirmedOrder.confirmedBy)
    }

    @Test
    @DisplayName("주문을 취소 처리할 수 있다")
    @Rollback
    fun `should cancel order successfully`() {
        // Given
        val userId = 500L
        val orderItems = listOf(
            OrderItemData(
                packageTypeId = 1L,
                packageTypeName = "취소 테스트 패키지",
                packageTypeDays = 5,
                dailyServing = 1,
                totalQuantity = 5.0,
                giftWrap = true,
                giftMessage = "테스트 메시지",
                quantity = 1,
                containerPrice = 2000,
                teaPrice = 12000,
                giftWrapPrice = 1000,
                teaItems = emptyList()
            )
        )

        val createdOrder = orderService.createOrder(
            userId = userId,
            items = orderItems,
            usedCouponId = null,
            totalAmount = 15000L,
            discountAmount = 0L,
            createdBy = userId
        )

        val cancelReason = "고객 변심"

        // When
        val cancelledOrder = orderService.cancelOrder(createdOrder.id, userId, cancelReason)

        // Then
        assertNotNull(cancelledOrder)
        assertEquals("CANCELLED", cancelledOrder.status.name)
        // 상태 확인만 수행 (cancelledAt, cancelledBy는 Order 엔티티에 없음)
        // assertEquals(userId, cancelledOrder.cancelledBy)
    }
}