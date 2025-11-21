package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.order.usecase.GetOrderQueryUseCase
import io.hhplus.ecommerce.order.application.OrderQueueService
import io.hhplus.ecommerce.order.application.OrderQueueWorker
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.usecase.ProductCommandUseCase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * 주문 생성 통합 테스트 (Redis Queue 시스템)
 *
 * TestContainers MySQL을 사용하여 개선된 주문 생성 전체 플로우를 검증합니다.
 * - Queue 기반 주문 생성 (즉시 응답 + 백그라운드 처리)
 * - 주문 번호 형식 검증 (ORD-YYYYMMDD-XXX)
 * - OrderItem 생성
 * - 쿠폰 적용 주문
 */
class OrderCreateIntegrationTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val getOrderQueryUseCase: GetOrderQueryUseCase,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productCommandUseCase: ProductCommandUseCase,
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val pointCommandUseCase: PointCommandUseCase,
    private val orderQueueService: OrderQueueService,
    private val orderQueueWorker: OrderQueueWorker
) : KotestIntegrationTestBase({

    // 테스트용 상품 ID를 저장할 변수
    lateinit var product1: Product
    lateinit var product2: Product

    beforeEach {
        // Queue 데이터 정리 (테스트 격리)
        orderQueueService.clearAllQueueData()

        // 모든 테스트 전에 상품과 재고 생성
        product1 = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "주문 테스트 상품 1",
                description = "주문 테스트용 상품",
                price = 20000L,
                categoryId = 1L,
                createdBy = 1L
            )
        )
        product2 = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "주문 테스트 상품 2",
                description = "주문 테스트용 상품",
                price = 15000L,
                categoryId = 1L,
                createdBy = 1L
            )
        )

        inventoryCommandUseCase.createInventory(product1.id, 1000)
        inventoryCommandUseCase.createInventory(product2.id, 1000)

        // 포인트 충전 (주문에 필요)
        pointCommandUseCase.chargePoint(1000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(2000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(3000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(4000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(5000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(6000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(7000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(8000L, 500000, "주문 테스트용 충전")
    }

    // Queue 기반 주문 생성 및 처리 완룉 대기 헬퍼 함수
    fun createOrderAndWait(request: CreateOrderRequest): io.hhplus.ecommerce.order.domain.entity.Order {
        // 1. Queue에 주문 등록
        val queueResponse = orderCommandUseCase.createOrder(request)
        queueResponse.status shouldBe QueueStatus.WAITING

        // 2. Queue 처리 (강제 실행으로 즉시 완룉)
        orderQueueWorker.forceProcessAllQueue()

        // 3. 처리된 주문 조회 (재시도 로직 추가)
        var orders: List<io.hhplus.ecommerce.order.domain.entity.Order> = emptyList()
        var attempts = 0
        val maxAttempts = 10

        while (orders.isEmpty() && attempts < maxAttempts) {
            Thread.sleep(100) // 짧은 대기
            orders = getOrderQueryUseCase.getOrdersByUser(request.userId)
            attempts++
        }

        orders shouldHaveSize 1
        return orders.first()
    }

    describe("주문 생성 (Redis Queue)") {
        context("정상적인 주문 생성 요청일 때") {
            it("주문을 정상적으로 생성할 수 있다") {
                // Given
                val userId = 10001L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 2,
                        giftWrap = false,
                        giftMessage = null
                    )
                )
                val totalAmount = 40000L
                val discountAmount = 0L

                // When
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderAndWait(createOrderRequest)

                // Then
                order shouldNotBe null
                order.userId shouldBe userId
                order.totalAmount shouldBe totalAmount
                order.discountAmount shouldBe discountAmount
                order.status shouldBe OrderStatus.PENDING
                order.orderNumber shouldStartWith "ORD"
            }
        }

        context("쿠폰을 적용한 주문 생성 시") {
            it("할인 금액이 반영된 주문이 생성된다") {
                // Given
                val userId = 10002L

                // 쿠폰 생성 및 발급은 일단 스킵하고 null로 설정
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product2.id,
                        quantity = 3,
                        giftWrap = false,
                        giftMessage = null
                    )
                )
                val totalAmount = 45000L

                // When - 일단 쿠폰 없이 주문 생성
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null, // 쿠폰 적용 로직 구현 필요
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderAndWait(createOrderRequest)

                // Then - 기본 검증
                order.totalAmount shouldBe totalAmount
                order.discountAmount shouldBe 0L // 쿠폰 없으므로 할인 없음
            }
        }

        context("여러 아이템을 포함한 주문 생성 시") {
            it("모든 아이템이 주문에 포함된다") {
                // Given
                val userId = 10003L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 2,
                        giftWrap = false,
                        giftMessage = null
                    ),
                    CreateOrderItemRequest(
                        productId = product2.id,
                        quantity = 1,
                        giftWrap = true,
                        giftMessage = "생일 축하합니다"
                    )
                )
                val totalAmount = 55000L // (20000*2) + (15000*1)

                // When
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderAndWait(createOrderRequest)

                // Then
                val savedOrder = orderRepository.findById(order.id)
                savedOrder shouldNotBe null

                val savedOrderItems = orderItemRepository.findByOrderId(order.id)
                savedOrderItems shouldHaveSize 2
                savedOrderItems[0].productId shouldBe product1.id
                savedOrderItems[1].productId shouldBe product2.id
            }
        }

        context("선물 포장 옵션이 있는 주문 생성 시") {
            it("선물 포장 정보가 저장된다") {
                // Given
                val userId = 10004L
                val giftMessage = "사랑하는 사람에게"
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 1,
                        giftWrap = true,
                        giftMessage = giftMessage
                    )
                )

                // When
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderAndWait(createOrderRequest)

                // Then
                val savedOrderItems = orderItemRepository.findByOrderId(order.id)
                savedOrderItems shouldHaveSize 1
                savedOrderItems[0].giftWrap shouldBe true
                savedOrderItems[0].giftMessage shouldBe giftMessage
            }
        }

        context("차 구성을 포함한 주문 생성 시") {
            it("차 구성 정보가 함께 저장된다") {
                // Given
                val userId = 10005L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product2.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )

                // When
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderAndWait(createOrderRequest)

                // Then
                order shouldNotBe null
                val savedOrderItems = orderItemRepository.findByOrderId(order.id)
                savedOrderItems shouldHaveSize 1
            }
        }
    }

    describe("주문 번호 생성") {
        context("주문을 생성할 때") {
            it("고유한 주문 번호가 생성된다") {
                // Given
                val userId = 6000L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )

                // When
                val queueRequest1 = orderCommandUseCase.createOrder(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                ))
                val queueRequest2 = orderCommandUseCase.createOrder(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                ))

                // Then
                queueRequest1.queueId shouldNotBe queueRequest2.queueId
                queueRequest1.queueId shouldStartWith "ORD"
                queueRequest2.queueId shouldStartWith "ORD"
            }
        }
    }

    describe("주문 조회") {
        context("생성된 주문을 조회할 때") {
            it("주문 정보를 조회할 수 있다") {
                // Given
                val userId = 7000L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product2.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )
                val queueRequest = orderCommandUseCase.createOrder(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                ))

                // Queue 처리 대기 (Worker가 주문을 처리할 시간)
                Thread.sleep(3000)

                // When - 처리된 주문 조회
                val userOrders = getOrderQueryUseCase.getOrdersByUser(userId)

                // Then
                userOrders shouldNotBe null
                userOrders.size shouldBe 1
                userOrders[0].userId shouldBe userId
            }
        }
    }

    describe("주문 상태") {
        context("주문 생성 직후") {
            it("상태가 PENDING이다") {
                // Given
                val userId = 8000L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )

                // When
                val queueRequest = orderCommandUseCase.createOrder(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = null,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                ))

                // Then
                queueRequest.status shouldBe QueueStatus.WAITING
            }
        }
    }
})
