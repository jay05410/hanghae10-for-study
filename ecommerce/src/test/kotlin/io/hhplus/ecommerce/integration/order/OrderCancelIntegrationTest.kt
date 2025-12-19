package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.application.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.presentation.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.hhplus.ecommerce.point.application.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.inventory.application.usecase.GetInventoryQueryUseCase
import io.hhplus.ecommerce.product.application.usecase.ProductCommandUseCase
import io.hhplus.ecommerce.inventory.application.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.product.presentation.dto.CreateProductRequest
import io.hhplus.ecommerce.common.outbox.infra.OutboxEventProcessor
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.inventory.application.usecase.StockReservationCommandUseCase
import io.hhplus.ecommerce.inventory.domain.service.StockReservationDomainService
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
    private val outboxEventProcessor: OutboxEventProcessor,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val stockReservationCommandUseCase: StockReservationCommandUseCase,
    private val stockReservationDomainService: StockReservationDomainService
) : KotestIntegrationTestBase({

    describe("주문 취소") {
        context("결제 전 PENDING 주문을 취소할 때") {
            it("재고 예약이 해제되어야 한다") {
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
                val savedInventory = inventoryCommandUseCase.restockInventory(
                    productId = savedProduct.id,
                    quantity = 100
                )
                val initialStock = savedInventory.quantity

                // 포인트 충전
                chargePointUseCase.execute(userId, 200000, "테스트용 충전")
                val initialBalance = getPointQueryUseCase.getUserPoint(userId).balance.value

                // 주문 생성
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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // 재고 예약 (체크아웃 흐름에서 수행되는 작업)
                val reservation = stockReservationCommandUseCase.reserveStock(
                    productId = savedProduct.id,
                    userId = userId,
                    quantity = orderQuantity,
                    reservationMinutes = 10
                )
                reservation.linkToOrder(createdOrder.id)
                stockReservationDomainService.save(reservation)

                // ORDER_CREATED 이벤트 처리 (배송 레코드 생성 - PENDING 상태)
                repeat(2) { outboxEventProcessor.processEvents() }

                // 주문 후 가용 재고 확인 (예약으로 인해 감소)
                val stockAfterOrder = getInventoryQueryUseCase.getAvailableQuantity(savedProduct.id)
                stockAfterOrder shouldBe (initialStock - orderQuantity)

                // 결제 전이므로 포인트는 아직 차감되지 않음
                val balanceAfterOrder = getPointQueryUseCase.getUserPoint(userId).balance.value
                balanceAfterOrder shouldBe initialBalance

                // When: PENDING 상태에서 주문 취소
                val cancelledOrder = orderCommandUseCase.cancelOrder(
                    orderId = createdOrder.id,
                    reason = "단순 변심"
                )

                // Saga 이벤트 처리: OrderCancelled → 재고 예약 해제
                repeat(2) { outboxEventProcessor.processEvents() }

                // Then: 주문 상태 확인
                cancelledOrder.status shouldBe OrderStatus.CANCELLED

                // 재고 예약 해제 확인 (가용 재고 복구)
                val stockAfterCancel = getInventoryQueryUseCase.getAvailableQuantity(savedProduct.id)
                stockAfterCancel shouldBe initialStock

                // 포인트는 차감된 적 없으므로 그대로 유지
                val balanceAfterCancel = getPointQueryUseCase.getUserPoint(userId).balance.value
                balanceAfterCancel shouldBe initialBalance
            }
        }

        context("배송 준비가 시작된 주문을 취소하려 할 때") {
            it("취소할 수 없어야 한다") {
                // Given: 주문 생성 후 결제 완료 (배송이 자동으로 PREPARING 상태로 전환됨)
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

                inventoryCommandUseCase.restockInventory(
                    productId = savedProduct.id,
                    quantity = 100
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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // 재고 예약
                val reservation = stockReservationCommandUseCase.reserveStock(
                    productId = savedProduct.id,
                    userId = userId,
                    quantity = 1,
                    reservationMinutes = 10
                )
                reservation.linkToOrder(createdOrder.id)
                stockReservationDomainService.save(reservation)

                // 결제 처리 → PaymentCompleted 이벤트 발행
                processPaymentUseCase.execute(
                    ProcessPaymentRequest(
                        userId = userId,
                        orderId = createdOrder.id,
                        amount = createdOrder.finalAmount,
                        paymentMethod = PaymentMethod.BALANCE
                    )
                )

                // Saga 이벤트 처리: PaymentCompleted → 배송 자동 PREPARING 전환
                // DeliveryEventHandler.handlePaymentCompleted()가 배송 상태를 PREPARING으로 변경
                repeat(3) { outboxEventProcessor.processEvents() }

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
