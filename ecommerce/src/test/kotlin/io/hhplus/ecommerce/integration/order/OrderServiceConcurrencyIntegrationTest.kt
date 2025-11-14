package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldNotBeEmpty
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * 주문 동시성 통합 테스트
 *
 * TestContainers MySQL을 사용하여 주문 동시 처리 정합성을 검증합니다.
 * - 동시 주문 생성
 * - 동시 같은 사용자 주문 생성
 * - 주문 생성과 조회 동시 처리
 */
class OrderServiceConcurrencyIntegrationTest(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository
) : KotestIntegrationTestBase({

    describe("동시성 주문 서비스") {
        context("동시에 여러 주문을 생성할 때") {
            it("모든 주문이 정상 생성되어야 한다") {
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
                orderIds shouldHaveSize threadCount
                orderIds.distinct() shouldHaveSize threadCount // 모든 ID가 유니크해야 함

                // 데이터베이스에서 실제 저장된 주문 확인
                val savedOrders = orderIds.mapNotNull { orderRepository.findById(it) }
                savedOrders shouldHaveSize threadCount

                // 각 주문의 데이터 정합성 확인
                savedOrders.forEach { order ->
                    (order.userId >= 1000L && order.userId < 1000L + threadCount) shouldBe true
                    order.totalAmount shouldBe 18000L
                    order.discountAmount shouldBe 0L
                    order.finalAmount shouldBe 18000L
                    order.orderNumber.shouldNotBeEmpty()
                }

                executor.shutdown()
            }
        }

        context("동시에 같은 사용자가 여러 주문을 생성할 때") {
            it("모든 주문이 정상 생성되어야 한다") {
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
                orderIds shouldHaveSize threadCount
                orderIds.distinct() shouldHaveSize threadCount

                // 사용자별 주문 목록 조회로 검증
                val userOrders = orderService.getOrdersByUser(userId)
                userOrders shouldHaveSize threadCount

                userOrders.forEach { order ->
                    order.userId shouldBe userId
                    order.totalAmount shouldBe 32000L
                    order.discountAmount shouldBe 2000L
                    order.finalAmount shouldBe 30000L
                }

                executor.shutdown()
            }
        }

        context("동시에 주문 생성과 조회가 발생할 때") {
            it("데이터 일관성이 유지되어야 한다") {
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
                createdOrderIds shouldHaveSize createThreadCount
                readResults.all { it >= 0 } shouldBe true // 읽기 작업이 에러 없이 완료

                // 최종 데이터 일관성 확인
                val finalOrders = orderService.getOrdersByUser(userId)
                finalOrders shouldHaveSize createThreadCount

                finalOrders.forEach { order ->
                    order.userId shouldBe userId
                    order.totalAmount shouldBe 48000L
                    createdOrderIds.contains(order.id) shouldBe true
                }

                executor.shutdown()
            }
        }
    }
})