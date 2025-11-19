package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.point.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.inventory.usecase.GetInventoryQueryUseCase
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.delivery.application.DeliveryService
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
    private val pointCommandUseCase: PointCommandUseCase,
    private val getPointQueryUseCase: GetPointQueryUseCase,
    private val getInventoryQueryUseCase: GetInventoryQueryUseCase,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val deliveryService: DeliveryService
) : KotestIntegrationTestBase({

    describe("주문 취소") {
        context("정상적인 주문을 취소할 때") {
            it("재고가 복구되고 포인트가 환불되어야 한다") {
                // Given: 사용자, 상품, 재고 준비
                val userId = 1000L
                val orderQuantity = 3

                // 상품 생성
                val product = Product.create(
                    name = "테스트 티",
                    description = "취소 테스트용 상품",
                    price = 10000L,
                    categoryId = 1L
                )
                val savedProduct = productRepository.save(product)

                // 재고 생성
                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )
                val savedInventory = inventoryRepository.save(inventory)
                val initialStock = savedInventory.quantity

                // 포인트 충전
                pointCommandUseCase.chargePoint(userId, 200000, "테스트용 충전")
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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

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

        context("이미 배송 시작된 주문을 취소하려 할 때") {
            it("취소할 수 없어야 한다") {
                // Given: 주문 생성 후 확정
                val userId = 1001L

                val product = Product.create(
                    name = "배송 테스트 티",
                    description = "배송 테스트용",
                    price = 10000L,
                    categoryId = 1L
                )
                val savedProduct = productRepository.save(product)

                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 100
                )
                inventoryRepository.save(inventory)

                pointCommandUseCase.chargePoint(userId, 100000, "테스트용 충전")

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

                val createdOrder = orderCommandUseCase.createOrder(createOrderRequest)

                // 주문 확정 (배송 시작)
                orderCommandUseCase.confirmOrder(createdOrder.id)

                // 배송 준비 시작 (이제 취소 불가)
                val delivery = deliveryService.getDeliveryByOrderId(createdOrder.id)
                delivery.let { deliveryService.startPreparing(it.id) }

                // When & Then: 배송 준비 중인 주문은 취소 불가
                val exception = runCatching {
                    orderCommandUseCase.cancelOrder(createdOrder.id, "취소 시도")
                }.exceptionOrNull()

                exception shouldNotBe null
                // 예외 타입 검증 (프로젝트의 예외 클래스에 맞게 수정)
                exception!!.message shouldContain "취소할 수 없는 주문 상태입니다"
                exception.message shouldContain "CONFIRMED"
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
