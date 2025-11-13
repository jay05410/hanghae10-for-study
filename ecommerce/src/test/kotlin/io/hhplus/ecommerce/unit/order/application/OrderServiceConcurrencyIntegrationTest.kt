package io.hhplus.ecommerce.unit.order.application

import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.support.IntegrationTestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * OrderService 동시성 통합 테스트
 *
 * TestContainers MySQL을 사용하여 실제 데이터베이스 환경에서 동시성 시나리오를 검증합니다.
 * 여러 스레드에서 동시에 주문을 생성하고, 데이터 정합성을 확인합니다.
 */
@DisplayName("주문 서비스 동시성 통합 테스트")
class OrderServiceConcurrencyIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Test
    @DisplayName("동시에 여러 주문을 생성할 때 모든 주문이 정상 생성되어야 한다")
    @Rollback
    fun `should handle concurrent order creation successfully`() {
        // Given
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val futures = mutableListOf<CompletableFuture<Long>>()

        val orderItemData = OrderItemData(
            packageTypeId = 1L,
            packageTypeName = "동시성 테스트 패키지",
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

        // When: 동시에 여러 주문 생성
        repeat(threadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                val userId = 1000L + index
                val order = orderService.createOrder(
                    userId = userId,
                    items = listOf(orderItemData),
                    usedCouponId = null,
                    totalAmount = 18000L,
                    discountAmount = 0L,
                    createdBy = userId
                )
                order.id
            }, executor)
            futures.add(future)
        }

        // 모든 작업 완료 대기
        val orderIds = futures.map { it.get() }

        // Then
        assertEquals(threadCount, orderIds.size)
        assertEquals(threadCount, orderIds.distinct().size) // 모든 ID가 유니크해야 함

        // 데이터베이스에서 실제 저장된 주문 확인
        val savedOrders = orderIds.mapNotNull { orderRepository.findById(it) }
        assertEquals(threadCount, savedOrders.size)

        // 각 주문의 데이터 정합성 확인
        savedOrders.forEach { order ->
            assertTrue(order.userId >= 1000L && order.userId < 1000L + threadCount)
            assertEquals(18000L, order.totalAmount)
            assertEquals(0L, order.discountAmount)
            assertEquals(18000L, order.finalAmount)
            assertTrue(order.orderNumber.isNotEmpty())
        }

        executor.shutdown()
    }

    @Test
    @DisplayName("동시에 같은 사용자가 여러 주문을 생성할 때 모든 주문이 정상 생성되어야 한다")
    @Rollback
    fun `should handle concurrent orders from same user successfully`() {
        // Given
        val threadCount = 5
        val userId = 2000L
        val executor = Executors.newFixedThreadPool(threadCount)
        val futures = mutableListOf<CompletableFuture<Long>>()

        val orderItemData = OrderItemData(
            packageTypeId = 2L,
            packageTypeName = "같은 사용자 동시성 테스트",
            packageTypeDays = 14,
            dailyServing = 2,
            totalQuantity = 28.0,
            giftWrap = true,
            giftMessage = "동시성 테스트",
            quantity = 1,
            containerPrice = 5000,
            teaPrice = 25000,
            giftWrapPrice = 2000,
            teaItems = emptyList()
        )

        // When: 같은 사용자가 동시에 여러 주문 생성
        repeat(threadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                val order = orderService.createOrder(
                    userId = userId,
                    items = listOf(orderItemData),
                    usedCouponId = null,
                    totalAmount = 32000L,
                    discountAmount = 2000L,
                    createdBy = userId
                )
                order.id
            }, executor)
            futures.add(future)
        }

        // 모든 작업 완료 대기
        val orderIds = futures.map { it.get() }

        // Then
        assertEquals(threadCount, orderIds.size)
        assertEquals(threadCount, orderIds.distinct().size)

        // 사용자별 주문 목록 조회로 검증
        val userOrders = orderService.getOrdersByUser(userId)
        assertEquals(threadCount, userOrders.size)

        userOrders.forEach { order ->
            assertEquals(userId, order.userId)
            assertEquals(32000L, order.totalAmount)
            assertEquals(2000L, order.discountAmount)
            assertEquals(30000L, order.finalAmount)
        }

        executor.shutdown()
    }

    @Test
    @DisplayName("동시에 주문 생성과 조회가 발생할 때 데이터 일관성이 유지되어야 한다")
    @Rollback
    fun `should maintain data consistency during concurrent create and read operations`() {
        // Given
        val createThreadCount = 3
        val readThreadCount = 2
        val userId = 3000L
        val executor = Executors.newFixedThreadPool(createThreadCount + readThreadCount)

        val orderItemData = OrderItemData(
            packageTypeId = 3L,
            packageTypeName = "읽기/쓰기 동시성 테스트",
            packageTypeDays = 10,
            dailyServing = 1,
            totalQuantity = 10.0,
            giftWrap = false,
            giftMessage = null,
            quantity = 2,
            containerPrice = 4000,
            teaPrice = 20000,
            giftWrapPrice = 0,
            teaItems = emptyList()
        )

        val createFutures = mutableListOf<CompletableFuture<Long>>()
        val readFutures = mutableListOf<CompletableFuture<Int>>()

        // When: 주문 생성과 조회 동시 실행
        // 주문 생성
        repeat(createThreadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                Thread.sleep(100L * index) // 약간의 지연으로 순차적 생성
                val order = orderService.createOrder(
                    userId = userId,
                    items = listOf(orderItemData),
                    usedCouponId = null,
                    totalAmount = 48000L,
                    discountAmount = 0L,
                    createdBy = userId
                )
                order.id
            }, executor)
            createFutures.add(future)
        }

        // 주문 조회 (생성 중에 계속 조회)
        repeat(readThreadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                var totalReads = 0
                repeat(5) {
                    Thread.sleep(50L)
                    val orders = orderService.getOrdersByUser(userId)
                    totalReads += orders.size
                }
                totalReads
            }, executor)
            readFutures.add(future)
        }

        // 모든 작업 완료 대기
        val createdOrderIds = createFutures.map { it.get() }
        val readResults = readFutures.map { it.get() }

        // Then
        assertEquals(createThreadCount, createdOrderIds.size)
        assertTrue(readResults.all { it >= 0 }) // 읽기 작업이 에러 없이 완료

        // 최종 데이터 일관성 확인
        val finalOrders = orderService.getOrdersByUser(userId)
        assertEquals(createThreadCount, finalOrders.size)

        finalOrders.forEach { order ->
            assertEquals(userId, order.userId)
            assertEquals(48000L, order.totalAmount)
            assertTrue(createdOrderIds.contains(order.id))
        }

        executor.shutdown()
    }
}