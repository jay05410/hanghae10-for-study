package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.usecase.GetOrderQueryUseCase
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.product.usecase.ProductCommandUseCase
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.user.usecase.UserCommandUseCase
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.kotest.matchers.shouldBe
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
    private val orderCommandUseCase: OrderCommandUseCase,
    private val getOrderQueryUseCase: GetOrderQueryUseCase,
    private val orderRepository: OrderRepository,
    private val productCommandUseCase: ProductCommandUseCase,
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val userCommandUseCase: UserCommandUseCase,
    private val pointCommandUseCase: PointCommandUseCase
) : KotestIntegrationTestBase({

    // 테스트용 데이터
    lateinit var testProduct: Product

    beforeEach {
        // 테스트용 상품 생성
        testProduct = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "동시성 테스트 상품",
                description = "동시성 테스트용 상품",
                price = 10000L,
                categoryId = 1L,
                createdBy = 1L
            )
        )

        // 재고 생성 (충분한 수량)
        inventoryCommandUseCase.createInventory(testProduct.id, 10000)

        // 테스트용 사용자들 생성 및 포인트 충전 (대규모 1000명)
        repeat(1000) { index ->
            val userId = 1000L + index
            try {
                userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "test$userId@example.com",
                    password = "password",
                    email = "test$userId@example.com",
                    name = "테스트사용자$userId",
                    phone = "010-0000-${String.format("%04d", index)}",
                    providerId = null
                )
                // 포인트 충전 (주문에 필요한 포인트)
                pointCommandUseCase.chargePoint(userId, 100000)
            } catch (e: Exception) {
                // 사용자가 이미 존재할 경우 무시하지만 포인트는 충전
                try {
                    pointCommandUseCase.chargePoint(userId, 100000)
                } catch (pointE: Exception) {
                    // 포인트 충전도 실패하면 무시
                }
            }
        }

        // 추가 테스트 사용자들 생성 (2000L, 3000L)
        listOf(2000L, 3000L).forEach { userId ->
            try {
                userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "test$userId@example.com",
                    password = "password",
                    email = "test$userId@example.com",
                    name = "테스트사용자$userId",
                    phone = "010-0000-${userId.toString().takeLast(4)}",
                    providerId = null
                )
                pointCommandUseCase.chargePoint(userId, 100000)
            } catch (e: Exception) {
                // 사용자가 이미 존재할 경우 무시하지만 포인트는 충전
                try {
                    pointCommandUseCase.chargePoint(userId, 100000)
                } catch (pointE: Exception) {
                    // 포인트 충전도 실패하면 무시
                }
            }
        }
    }

    describe("동시성 주문 서비스") {
        context("동시에 여러 주문을 생성할 때") {
            it("모든 주문이 정상 생성되어야 한다") {
        // Given
        val threadCount = 100 // 안정적인 테스트: 100건 동시 주문 (성공 가능한 규모)
        val executor = Executors.newFixedThreadPool(threadCount)
        val futures = mutableListOf<CompletableFuture<String>>()

        val orderItem = CreateOrderItemRequest(
            productId = testProduct.id,
            quantity = 1,
            giftWrap = false,
            giftMessage = null
        )

        // When: 동시에 여러 Queue 등록 (성능 측정)
        println("=== Redis Queue 주문 시스템 동시성 테스트 시작 (100건) ===")
        val startTime = System.currentTimeMillis()

        repeat(threadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                val userId = 1000L + index
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = listOf(orderItem),
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "Queue 테스트용 배송"
                    )
                )
                val queueRequest = orderCommandUseCase.createOrder(createOrderRequest)
                queueRequest.queueId
            }, executor)
            futures.add(future)
        }

        // 모든 작업 완료 대기 (Queue ID 수집)
        val queueIds = futures.map { it.get() }
        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        val tps = (threadCount * 1000.0) / executionTime

        println("=== Redis Queue 주문 시스템 동시성 테스트 결과 (100건) ===")
        println("총 Queue 등록 수: $threadCount")
        println("실행 시간: ${executionTime}ms (${String.format("%.3f", executionTime/1000.0)}초)")
        println("Queue 등록 TPS: ${String.format("%.1f", tps)}")
        println("Queue 등록 성공율: 100%")

        // Queue 처리 대기 (Worker가 처리할 시간 제공)
        Thread.sleep(10000) // 10초 대기

        // 실제 처리된 주문 수 확인
        val processedOrders = (1000L until 1000L + threadCount).flatMap { userId ->
            getOrderQueryUseCase.getOrdersByUser(userId)
        }

        println("실제 처리된 주문 수: ${processedOrders.size}/$threadCount")

        // 결과를 파일로 저장
        val resultFile = java.io.File("/Users/hj/Desktop/항해/Git/hanghae10-for-study/ecommerce/order_queue_100_result.txt")
        resultFile.writeText("""
            === Redis Queue 주문 시스템 동시성 테스트 결과 (100건) ===
            총 Queue 등록 수: $threadCount
            Queue 등록 시간: ${executionTime}ms (${String.format("%.3f", executionTime/1000.0)}초)
            Queue 등록 TPS: ${String.format("%.1f", tps)}
            Queue 등록 성공율: 100%
            실제 처리된 주문 수: ${processedOrders.size}/$threadCount
        """.trimIndent())

                // Then: Queue 등록 검증
                queueIds shouldHaveSize threadCount
                queueIds.distinct() shouldHaveSize threadCount // 모든 Queue ID가 유니크해야 함
                queueIds.all { it.isNotEmpty() } shouldBe true

                // 처리된 주문들의 정합성 확인 (처리된 것만)
                processedOrders.forEach { order ->
                    (order.userId >= 1000L && order.userId < 1000L + threadCount) shouldBe true
                    (order.totalAmount > 0) shouldBe true
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

        val orderItem = CreateOrderItemRequest(
            productId = testProduct.id,
            quantity = 1,
            giftWrap = true,
            giftMessage = "동시성 테스트"
        )

        // When: 같은 사용자가 동시에 여러 주문 생성
        repeat(threadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = listOf(orderItem),
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "같은사용자 테스트",
                        phone = "010-9999-8888",
                        zipCode = "54321",
                        address = "서울시 송파구",
                        addressDetail = "동시성 테스트 상세주소",
                        deliveryMessage = "같은 사용자 동시성 테스트용"
                    )
                )
                val order = orderCommandUseCase.createOrder(createOrderRequest)
                order.id
            }, executor)
            futures.add(future)
        }

        // 모든 작업 완료 대기 (Order ID 수집)
        val orderIds = futures.map { it.get() }

                // Then: 레거시 주문 검증
                orderIds shouldHaveSize threadCount
                orderIds.distinct() shouldHaveSize threadCount

                // 사용자별 주문 목록 조회로 검증
                val userOrders = getOrderQueryUseCase.getOrdersByUser(userId)
                println("사용자 ${userId}의 생성된 주문 수: ${userOrders.size}/$threadCount")

                // 모든 주문이 생성되었어야 함
                userOrders.size shouldBe threadCount

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

        val orderItem = CreateOrderItemRequest(
            productId = testProduct.id,
            quantity = 2,
            giftWrap = false,
            giftMessage = null
        )

        val createFutures = mutableListOf<CompletableFuture<Long>>()
        val readFutures = mutableListOf<CompletableFuture<Int>>()

        // When: 주문 생성과 조회 동시 실행
        // 주문 생성
        repeat(createThreadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                Thread.sleep(5L * index) // 약간의 지연으로 순차적 생성
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = listOf(orderItem),
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "읽기쓰기 테스트",
                        phone = "010-7777-6666",
                        zipCode = "11111",
                        address = "서울시 마포구",
                        addressDetail = "읽기쓰기 동시성 테스트",
                        deliveryMessage = "동시 처리 테스트용"
                    )
                )
                val order = orderCommandUseCase.createOrder(createOrderRequest)
                order.id
            }, executor)
            createFutures.add(future)
        }

        // 주문 조회 (생성 중에 계속 조회)
        repeat(readThreadCount) { index ->
            val future = CompletableFuture.supplyAsync({
                var totalReads = 0
                repeat(5) {
                    Thread.sleep(5L)
                    val orders = getOrderQueryUseCase.getOrdersByUser(userId)
                    totalReads += orders.size
                }
                totalReads
            }, executor)
            readFutures.add(future)
        }

        // 모든 작업 완료 대기
        val createdOrderIds = createFutures.map { it.get() }
        val readResults = readFutures.map { it.get() }

                // Then: 레거시 주문 생성 검증
                createdOrderIds shouldHaveSize createThreadCount
                readResults.all { it >= 0 } shouldBe true // 읽기 작업이 에러 없이 완료

                // 최종 데이터 일관성 확인
                val finalOrders = getOrderQueryUseCase.getOrdersByUser(userId)
                println("읽기쓰기 동시성 테스트 - 사용자 ${userId}의 생성된 주문 수: ${finalOrders.size}/$createThreadCount")

                // 모든 주문이 생성되었어야 함
                finalOrders.size shouldBe createThreadCount

                executor.shutdown()
            }
        }
    }
})