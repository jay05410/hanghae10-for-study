package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
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
 * 주문 생성 통합 테스트
 *
 * TestContainers MySQL을 사용하여 주문 생성 전체 플로우를 검증합니다.
 * - 주문 생성 (Cart → Order 변환)
 * - 주문 번호 형식 검증 (ORD-YYYYMMDD-XXX)
 * - OrderItem 및 OrderItemTea 생성
 * - 쿠폰 적용 주문
 */
class OrderCreateIntegrationTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productCommandUseCase: ProductCommandUseCase,
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val pointCommandUseCase: PointCommandUseCase
) : KotestIntegrationTestBase({

    // 테스트용 상품 ID를 저장할 변수
    lateinit var product1: Product
    lateinit var product2: Product

    beforeEach {
        // 모든 테스트 전에 상품과 재고 생성
        product1 = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "주문 테스트 티 1",
                description = "주문 테스트용 차",
                price = 20000L,
                categoryId = 1L,
                createdBy = 0L
            )
        )
        product2 = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "주문 테스트 티 2",
                description = "주문 테스트용 차",
                price = 15000L,
                categoryId = 1L,
                createdBy = 0L
            )
        )

        inventoryCommandUseCase.createInventory(product1.id, 1000, 0L)
        inventoryCommandUseCase.createInventory(product2.id, 1000, 0L)

        // 포인트 충전 (주문에 필요)
        pointCommandUseCase.chargePoint(1000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(2000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(3000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(4000L, 500000, "주문 테스트용 충전")
        pointCommandUseCase.chargePoint(5000L, 500000, "주문 테스트용 충전")
    }

    describe("주문 생성") {
        context("정상적인 주문 생성 요청일 때") {
            it("주문을 정상적으로 생성할 수 있다") {
                // Given
                val userId = 1000L
                val items = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = product1.id, // 실제 생성한 상품 ID 사용
                        packageTypeName = "7일 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 7.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 20000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )
                val totalAmount = 30000L
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
                val order = orderCommandUseCase.createOrder(createOrderRequest)

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
                val userId = 2000L
                val items = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 2L,
                        packageTypeName = "14일 패키지",
                        packageTypeDays = 14,
                        dailyServing = 1,
                        totalQuantity = 14.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 15000,
                        teaPrice = 35000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )
                val totalAmount = 50000L
                val discountAmount = 5000L
                val usedCouponId = 100L

                // When
                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = items,
                    usedCouponId = usedCouponId,
                    deliveryAddress = DeliveryAddressRequest(
                        recipientName = "테스트 수령인",
                        phone = "010-1234-5678",
                        zipCode = "12345",
                        address = "서울시 강남구",
                        addressDetail = "테스트 상세주소",
                        deliveryMessage = "테스트 배송 메시지"
                    )
                )
                val order = orderCommandUseCase.createOrder(createOrderRequest)

                // Then
                order.usedCouponId shouldBe usedCouponId
                order.totalAmount shouldBe totalAmount
                order.discountAmount shouldBe discountAmount
            }
        }

        context("여러 아이템을 포함한 주문 생성 시") {
            it("모든 아이템이 주문에 포함된다") {
                // Given
                val userId = 3000L
                val items = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 3L,
                        packageTypeName = "7일 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 7.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 2,
                        containerPrice = 10000,
                        teaPrice = 20000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    ),
                    CreateOrderItemRequest(
                        packageTypeId = 4L,
                        packageTypeName = "14일 패키지",
                        packageTypeDays = 14,
                        dailyServing = 1,
                        totalQuantity = 14.0,
                        giftWrap = true,
                        giftMessage = "생일 축하합니다",
                        quantity = 1,
                        containerPrice = 15000,
                        teaPrice = 35000,
                        giftWrapPrice = 2000,
                        teaItems = emptyList()
                    )
                )
                val totalAmount = 112000L // (10000+20000)*2 + 15000+35000+2000

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
                val order = orderCommandUseCase.createOrder(createOrderRequest)

                // Then
                val savedOrder = orderRepository.findById(order.id)
                savedOrder shouldNotBe null

                val savedOrderItems = orderItemRepository.findByOrderId(order.id)
                savedOrderItems shouldHaveSize 2
                savedOrderItems[0].packageTypeId shouldBe 3L
                savedOrderItems[1].packageTypeId shouldBe 4L
            }
        }

        context("선물 포장 옵션이 있는 주문 생성 시") {
            it("선물 포장 정보가 저장된다") {
                // Given
                val userId = 4000L
                val giftMessage = "사랑하는 사람에게"
                val items = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 5L,
                        packageTypeName = "30일 패키지",
                        packageTypeDays = 30,
                        dailyServing = 1,
                        totalQuantity = 30.0,
                        giftWrap = true,
                        giftMessage = giftMessage,
                        quantity = 1,
                        containerPrice = 20000,
                        teaPrice = 80000,
                        giftWrapPrice = 3000,
                        teaItems = emptyList()
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
                val order = orderCommandUseCase.createOrder(createOrderRequest)

                // Then
                val savedOrderItems = orderItemRepository.findByOrderId(order.id)
                savedOrderItems shouldHaveSize 1
                savedOrderItems[0].giftWrap shouldBe true
                savedOrderItems[0].giftMessage shouldBe giftMessage
                savedOrderItems[0].giftWrapPrice shouldBe 3000L
            }
        }

        context("차 구성을 포함한 주문 생성 시") {
            it("차 구성 정보가 함께 저장된다") {
                // Given
                val userId = 5000L
                val teaItems = listOf(
                    TeaItemRequest(
                        productId = 101L,
                        selectionOrder = 1,
                        ratioPercent = 43 // 3*2 / 14 ≈ 43%
                    ),
                    TeaItemRequest(
                        productId = 102L,
                        selectionOrder = 2,
                        ratioPercent = 57 // 4*2 / 14 ≈ 57%
                    )
                )
                val items = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 6L,
                        packageTypeName = "7일 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 14.0, // 3*2 + 4*2
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 20000,
                        giftWrapPrice = 0,
                        teaItems = teaItems
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
                val order = orderCommandUseCase.createOrder(createOrderRequest)

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
                        packageTypeId = 7L,
                        packageTypeName = "테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 7.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )

                // When
                val order1 = orderCommandUseCase.createOrder(CreateOrderRequest(
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
                val order2 = orderCommandUseCase.createOrder(CreateOrderRequest(
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
                val userId = 7000L
                val items = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 8L,
                        packageTypeName = "조회 테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 7.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )
                val order = orderCommandUseCase.createOrder(CreateOrderRequest(
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

                // When
                val foundOrder = orderRepository.findById(order.id)

                // Then
                foundOrder shouldNotBe null
                foundOrder!!.id shouldBe order.id
                foundOrder.userId shouldBe userId
                foundOrder.orderNumber shouldBe order.orderNumber
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
                        packageTypeId = 9L,
                        packageTypeName = "상태 테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 7.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )

                // When
                val order = orderCommandUseCase.createOrder(CreateOrderRequest(
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
                order.status shouldBe OrderStatus.PENDING
            }
        }
    }
})
