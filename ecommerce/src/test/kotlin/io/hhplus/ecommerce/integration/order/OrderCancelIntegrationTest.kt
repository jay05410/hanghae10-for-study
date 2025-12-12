package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.application.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.presentation.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.hhplus.ecommerce.point.application.usecase.UsePointUseCase
import io.hhplus.ecommerce.point.application.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.inventory.application.usecase.GetInventoryQueryUseCase
import io.hhplus.ecommerce.product.application.usecase.ProductCommandUseCase
import io.hhplus.ecommerce.inventory.application.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.delivery.application.usecase.DeliveryCommandUseCase
import io.hhplus.ecommerce.delivery.application.usecase.GetDeliveryQueryUseCase
import io.hhplus.ecommerce.product.presentation.dto.CreateProductRequest
import io.hhplus.ecommerce.common.outbox.infra.OutboxEventProcessor
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * 주문 취소 통합 테스트
 *
 * 목적:
 * - 주문 취소 시 재고 복구 검증
 * - 포인트 환불 검증
 * - 주문 상태 변경 검증
 *
 * 시나리오:
 * 1. 주문 생성 (포인트 사용 + 재고 차감)
 * 2. 주문 취소
 * 3. 재고 복구 확인
 * 4. 포인트 환불 확인
 */
class OrderCancelIntegrationTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val chargePointUseCase: ChargePointUseCase,
    private val getPointQueryUseCase: GetPointQueryUseCase,
    private val getInventoryQueryUseCase: GetInventoryQueryUseCase,
    private val productCommandUseCase: ProductCommandUseCase,
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val deliveryCommandUseCase: DeliveryCommandUseCase,
    private val getDeliveryQueryUseCase: GetDeliveryQueryUseCase,
    private val outboxEventProcessor: OutboxEventProcessor
) : KotestIntegrationTestBase({

    describe("주문 취소") {
        context("정상적인 주문을 취소할 때") {
            it("재고가 복구되고 포인트가 환불되어야 한다") {
                // Given: 사용자, 상품, 재고 준비
                val userId = 20001L
                val orderQuantity = 3

                // 상품 생성
                val savedProduct = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "테스트 티",
                        description = "취소 테스트용 상품",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )

                // 재고 생성
                val savedInventory = inventoryCommandUseCase.createInventory(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )
                val initialStock = savedInventory.quantity

                // 포인트 충전
                chargePointUseCase.execute(userId, 200000, "테스트용 충전")
                val initialBalance = getPointQueryUseCase.getUserPoint(userId).balance.value

                // 주문 생성 (재고 차감 + 포인트 사용)
                val orderItems = listOf(
                    CreateOrderItemRequest(
                        productId = savedProduct.id,
                        quantity = orderQuantity,
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

                // 주문 생성 (직접 처리)
                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // Saga 이벤트 처리: OrderCreated → PaymentCompleted → 재고/포인트 처리
                repeat(3) { outboxEventProcessor.processEvents() }

                // 주문 후 재고 확인
                val stockAfterOrder = getInventoryQueryUseCase.getAvailableQuantity(savedProduct.id)
                stockAfterOrder shouldBe (initialStock - orderQuantity)

                // 주문 후 포인트 확인
                val balanceAfterOrder = getPointQueryUseCase.getUserPoint(userId).balance.value
                balanceAfterOrder shouldBe (initialBalance - (savedProduct.price * orderQuantity))

                // When: 주문 취소
                val cancelledOrder = orderCommandUseCase.cancelOrder(
                    orderId = createdOrder.id,
                    reason = "단순 변심"
                )

                // Saga 이벤트 처리: OrderCancelled → 재고 복구/포인트 환불
                repeat(2) { outboxEventProcessor.processEvents() }

                // Then: 주문 상태 확인
                cancelledOrder.status shouldBe OrderStatus.CANCELLED

                // 재고 복구 확인
                val stockAfterCancel = getInventoryQueryUseCase.getAvailableQuantity(savedProduct.id)
                stockAfterCancel shouldBe initialStock

                // 포인트 환불 확인
                val balanceAfterCancel = getPointQueryUseCase.getUserPoint(userId).balance.value
                balanceAfterCancel shouldBe initialBalance
            }
        }

        context("배송 준비가 시작된 주문을 취소하려 할 때") {
            it("취소할 수 없어야 한다") {
                // Given: 주문 생성 후 배송 준비 시작
                val userId = 20002L

                val savedProduct = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "배송 테스트 티",
                        description = "배송 테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )

                inventoryCommandUseCase.createInventory(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )

                chargePointUseCase.execute(userId, 100000, "테스트용 충전")

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

                // 주문 생성 (직접 처리)
                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // Saga 이벤트 처리: OrderCreated → PaymentCompleted → 배송 생성
                repeat(3) { outboxEventProcessor.processEvents() }

                // 배송 준비 시작 (PREPARING 상태 - 이후 취소 불가)
                val delivery = getDeliveryQueryUseCase.getDeliveryByOrderId(createdOrder.id)
                deliveryCommandUseCase.startPreparing(delivery.id)

                // When & Then: 배송 준비 시작 후에는 취소 불가
                val exception = runCatching {
                    orderCommandUseCase.cancelOrder(createdOrder.id, "취소 시도")
                }.exceptionOrNull()

                exception shouldNotBe null
                // 예외 타입 검증: 배송 상태로 인한 취소 불가
                exception!!.message shouldContain "배송 준비가 시작되어"
                exception.message shouldContain "PREPARING"
            }
        }

        context("존재하지 않는 주문을 취소하려 할 때") {
            it("예외가 발생해야 한다") {
                // Given
                val nonExistentOrderId = 99999L

                // When & Then
                val exception = runCatching {
                    orderCommandUseCase.cancelOrder(nonExistentOrderId, "취소")
                }.exceptionOrNull()

                exception shouldNotBe null
            }
        }
    }
})
