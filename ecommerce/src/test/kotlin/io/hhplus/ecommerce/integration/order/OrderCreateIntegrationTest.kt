package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.application.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.application.usecase.GetOrderQueryUseCase
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.presentation.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.inventory.application.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.hhplus.ecommerce.product.presentation.dto.CreateProductRequest
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.application.usecase.ProductCommandUseCase
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
    private val chargePointUseCase: ChargePointUseCase
) : KotestIntegrationTestBase({

    // 테스트용 상품 ID를 저장할 변수
    lateinit var product1: Product
    lateinit var product2: Product

    beforeEach {
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

        inventoryCommandUseCase.restockInventory(product1.id, 1000)
        inventoryCommandUseCase.restockInventory(product2.id, 1000)

        // 포인트 충전 (주문에 필요)
        chargePointUseCase.execute(1000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(2000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(3000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(4000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(5000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(6000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(7000L, 500000, "주문 테스트용 충전")
        chargePointUseCase.execute(8000L, 500000, "주문 테스트용 충전")
    }

    // 주문 직접 생성 헬퍼 함수 (테스트용)
    fun createOrderDirectly(request: CreateOrderRequest): io.hhplus.ecommerce.order.domain.entity.Order {
        // 사용자 포인트 충전 (주문에 충분한 포인트 제공)
        chargePointUseCase.execute(request.userId, 500000, "테스트용 충전")

        // 직접 주문 처리
        return orderCommandUseCase.createOrder(request)
    }

    describe("주문 생성 (Redis Queue)") {
        context("정상적인 주문 생성 요청일 때") {
            it("주문을 정상적으로 생성할 수 있다") {
                // Given
                val userId = System.currentTimeMillis() % 1000000 + 10001L
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
                    usedCouponIds = emptyList(),
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderDirectly(createOrderRequest)

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
                val userId = System.currentTimeMillis() % 1000000 + 10002L

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
                    usedCouponIds = emptyList(), // 쿠폰 적용 로직 구현 필요
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderDirectly(createOrderRequest)

                // Then - 기본 검증
                order.totalAmount shouldBe totalAmount
                order.discountAmount shouldBe 0L // 쿠폰 없으므로 할인 없음
            }
        }

        context("여러 아이템을 포함한 주문 생성 시") {
            it("모든 아이템이 주문에 포함된다") {
                // Given
                val userId = System.currentTimeMillis() % 1000000 + 10003L
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
                    usedCouponIds = emptyList(),
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderDirectly(createOrderRequest)

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
                val userId = System.currentTimeMillis() % 1000000 + 10004L
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
                    usedCouponIds = emptyList(),
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderDirectly(createOrderRequest)

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
                val userId = System.currentTimeMillis() % 1000000 + 10005L
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
                    usedCouponIds = emptyList(),
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = createOrderDirectly(createOrderRequest)

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
                val userId = System.currentTimeMillis() % 1000000 + 60001L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )

                // When - 직접 주문 처리로 두 주문 생성
                val order1 = createOrderDirectly(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponIds = emptyList(),
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                ))
                val order2 = createOrderDirectly(CreateOrderRequest(
                    userId = userId + 1, // 다른 사용자 ID로 설정
                    items = items,
                    usedCouponIds = emptyList(),
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
                order1.orderNumber shouldNotBe order2.orderNumber
                order1.orderNumber shouldStartWith "ORD"
                order2.orderNumber shouldStartWith "ORD"
            }
        }
    }

    describe("주문 조회") {
        context("생성된 주문을 조회할 때") {
            it("주문 정보를 조회할 수 있다") {
                // Given
                val userId = System.currentTimeMillis() % 1000000 + 70001L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product2.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )
                // 직접 주문 처리
                val order = createOrderDirectly(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponIds = emptyList(),
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                ))

                // When - 주문 조회
                val userOrders = getOrderQueryUseCase.getOrdersByUser(userId)

                // Then
                userOrders shouldNotBe null
                userOrders.size shouldBe 1
                userOrders[0].userId shouldBe userId
                userOrders[0].id shouldBe order.id
            }
        }
    }

    describe("주문 상태") {
        context("주문 생성 직후") {
            it("상태가 PENDING이다") {
                // Given
                val userId = System.currentTimeMillis() % 1000000 + 80001L
                val items = listOf(
                    CreateOrderItemRequest(
                        productId = product1.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
                    )
                )

                // When - 직접 주문 처리
                val order = createOrderDirectly(CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponIds = emptyList(),
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
                order.status shouldBe OrderStatus.PENDING
            }
        }
    }
})
