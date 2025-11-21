package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.product.usecase.ProductCommandUseCase
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldHaveSize

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
    private val productCommandUseCase: ProductCommandUseCase,
    private val inventoryCommandUseCase: InventoryCommandUseCase
) : KotestIntegrationTestBase({

    describe("주문 확정") {
        context("PENDING 상태의 주문을 확정할 때") {
            it("상태가 CONFIRMED로 변경되어야 한다") {
                // Given: 주문 생성
                val userId = 30001L

                val savedProduct = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "확정 테스트 티",
                        description = "확정 테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )

                inventoryCommandUseCase.createInventory(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )

                pointCommandUseCase.chargePoint(userId, 50000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
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

                // 주문 생성 (직접 처리 - 비즈니스 로직 테스트 목적)
                val createdOrder = orderCommandUseCase.processOrderDirectly(createOrderRequest)

                // 주문 생성 직후 상태 확인
                createdOrder.status shouldBe OrderStatus.PENDING

                // When: 주문 확정
                val confirmedOrder = orderCommandUseCase.confirmOrder(
                    orderId = createdOrder.id
                )

                // Then: 상태 변경 확인
                confirmedOrder.status shouldBe OrderStatus.CONFIRMED
            }
        }

        context("이미 확정된 주문을 다시 확정하려 할 때") {
            it("멱등성이 보장되거나 예외가 발생해야 한다") {
                // Given: 주문 생성 및 확정
                val userId = 30002L

                val savedProduct = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "중복 확정 테스트 티",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )

                inventoryCommandUseCase.createInventory(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )

                pointCommandUseCase.chargePoint(userId, 50000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
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

                // 주문 생성 (직접 처리 - 비즈니스 로직 테스트 목적)
                val createdOrder = orderCommandUseCase.processOrderDirectly(createOrderRequest)

                // 첫 번째 확정
                orderCommandUseCase.confirmOrder(createdOrder.id)

                // When & Then: 두 번째 확정 시도
                val exception = runCatching {
                    orderCommandUseCase.confirmOrder(createdOrder.id)
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
                val userId = 30003L

                val savedProduct = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "취소 후 확정 테스트 티",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )

                inventoryCommandUseCase.createInventory(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )

                pointCommandUseCase.chargePoint(userId, 50000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 1,
                        giftWrap = false,
                        giftMessage = null
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

                // 주문 생성 (직접 처리 - 비즈니스 로직 테스트 목적)
                val createdOrder = orderCommandUseCase.processOrderDirectly(createOrderRequest)

                // 주문 취소
                orderCommandUseCase.cancelOrder(createdOrder.id, "테스트 취소")

                // When & Then: 취소된 주문 확정 시도
                val exception = runCatching {
                    orderCommandUseCase.confirmOrder(createdOrder.id)
                }.exceptionOrNull()

                exception shouldNotBe null
                exception!!.message shouldContain "잘못된 주문 상태 변경입니다"
                exception.message shouldContain "CANCELLED"
                exception.message shouldContain "CONFIRMED"
            }
        }
    }
})
