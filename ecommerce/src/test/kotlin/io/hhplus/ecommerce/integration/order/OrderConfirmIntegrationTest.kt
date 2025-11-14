package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * 주문 확정 통합 테스트
 *
 * 목적:
 * - 주문 확정 시 상태 변경 검증
 * - 배송 시작 검증
 *
 * 시나리오:
 * 1. 주문 생성 (PENDING)
 * 2. 주문 확정
 * 3. 상태가 CONFIRMED로 변경되었는지 확인
 */
class OrderConfirmIntegrationTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val pointCommandUseCase: PointCommandUseCase,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository
) : KotestIntegrationTestBase({

    describe("주문 확정") {
        context("PENDING 상태의 주문을 확정할 때") {
            it("상태가 CONFIRMED로 변경되어야 한다") {
                // Given: 주문 생성
                val userId = 2000L

                val product = Product.create(
                    name = "확정 테스트 티",
                    description = "확정 테스트용",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 0L
                )
                val savedProduct = productRepository.save(product)

                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 100,
                    createdBy = 0L
                )
                inventoryRepository.save(inventory)

                pointCommandUseCase.chargePoint(userId, 50000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 1L,
                        packageTypeName = "확정 테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 1.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )

                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = orderItems,
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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // 주문 생성 직후 상태 확인
                createdOrder.status shouldBe OrderStatus.PENDING

                // When: 주문 확정
                val confirmedOrder = orderCommandUseCase.confirmOrder(
                    orderId = createdOrder.id,
                    confirmedBy = userId
                )

                // Then: 상태 변경 확인
                confirmedOrder.status shouldBe OrderStatus.CONFIRMED
                confirmedOrder.updatedBy shouldBe userId
            }
        }

        context("이미 확정된 주문을 다시 확정하려 할 때") {
            it("멱등성이 보장되거나 예외가 발생해야 한다") {
                // Given: 주문 생성 및 확정
                val userId = 2001L

                val product = Product.create(
                    name = "중복 확정 테스트 티",
                    description = "테스트용",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 0L
                )
                val savedProduct = productRepository.save(product)

                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 100,
                    createdBy = 0L
                )
                inventoryRepository.save(inventory)

                pointCommandUseCase.chargePoint(userId, 50000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 1L,
                        packageTypeName = "테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 1.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )

                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = orderItems,
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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // 첫 번째 확정
                orderCommandUseCase.confirmOrder(createdOrder.id, userId)

                // When & Then: 두 번째 확정 시도
                val exception = runCatching {
                    orderCommandUseCase.confirmOrder(createdOrder.id, userId)
                }.exceptionOrNull()

                // InvalidOrderStatus 예외 발생
                exception shouldNotBe null
                exception!!.message shouldContain "잘못된 주문 상태 변경입니다"
                exception.message shouldContain "CONFIRMED"
            }
        }

        context("CANCELLED 상태의 주문을 확정하려 할 때") {
            it("예외가 발생해야 한다") {
                // Given: 주문 생성 후 취소
                val userId = 2002L

                val product = Product.create(
                    name = "취소 후 확정 테스트 티",
                    description = "테스트용",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 0L
                )
                val savedProduct = productRepository.save(product)

                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 100,
                    createdBy = 0L
                )
                inventoryRepository.save(inventory)

                pointCommandUseCase.chargePoint(userId, 50000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = 1L,
                        packageTypeName = "테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 1.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = emptyList()
                    )
                )

                val createOrderRequest = CreateOrderRequest(
                    userId = userId,
                    items = orderItems,
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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // 주문 취소
                orderCommandUseCase.cancelOrder(createdOrder.id, userId, "테스트 취소")

                // When & Then: 취소된 주문 확정 시도
                val exception = runCatching {
                    orderCommandUseCase.confirmOrder(createdOrder.id, userId)
                }.exceptionOrNull()

                exception shouldNotBe null
                exception!!.message shouldContain "잘못된 주문 상태 변경입니다"
                exception.message shouldContain "CANCELLED"
                exception.message shouldContain "CONFIRMED"
            }
        }
    }
})
