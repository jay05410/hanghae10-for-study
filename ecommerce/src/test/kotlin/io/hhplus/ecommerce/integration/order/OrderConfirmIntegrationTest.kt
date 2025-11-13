package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.order.usecase.CancelOrderUseCase
import io.hhplus.ecommerce.order.usecase.ConfirmOrderUseCase
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

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
    private val orderService: OrderService,
    private val confirmOrderUseCase: ConfirmOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val pointService: PointService,
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

                pointService.earnPoint(userId, PointAmount(50000), userId)

                val orderItems = listOf(
                    OrderItemData(
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

                val createdOrder = orderService.createOrder(
                    userId = userId,
                    items = orderItems,
                    usedCouponId = null,
                    totalAmount = 20000L,
                    discountAmount = 0L,
                    createdBy = userId
                )

                // 주문 생성 직후 상태 확인
                createdOrder.status shouldBe OrderStatus.PENDING

                // When: 주문 확정
                val confirmedOrder = confirmOrderUseCase.execute(
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

                pointService.earnPoint(userId, PointAmount(50000), userId)

                val orderItems = listOf(
                    OrderItemData(
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

                val createdOrder = orderService.createOrder(
                    userId = userId,
                    items = orderItems,
                    usedCouponId = null,
                    totalAmount = 20000L,
                    discountAmount = 0L,
                    createdBy = userId
                )

                // 첫 번째 확정
                confirmOrderUseCase.execute(createdOrder.id, userId)

                // When & Then: 두 번째 확정 시도
                val result = runCatching {
                    confirmOrderUseCase.execute(createdOrder.id, userId)
                }

                // 멱등성 보장 또는 예외 발생
                result.isSuccess shouldBe true // 멱등성 보장하면 성공
                // 또는 예외 발생 검증 (프로젝트 정책에 따라)
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

                pointService.earnPoint(userId, PointAmount(50000), userId)

                val orderItems = listOf(
                    OrderItemData(
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

                val createdOrder = orderService.createOrder(
                    userId = userId,
                    items = orderItems,
                    usedCouponId = null,
                    totalAmount = 20000L,
                    discountAmount = 0L,
                    createdBy = userId
                )

                // 주문 취소
                cancelOrderUseCase.execute(createdOrder.id, userId, "테스트 취소")

                // When & Then: 취소된 주문 확정 시도
                val exception = runCatching {
                    confirmOrderUseCase.execute(createdOrder.id, userId)
                }.exceptionOrNull()

                exception shouldNotBe null
                exception!!.message shouldBe "취소된 주문은 확정할 수 없습니다"
            }
        }
    }
})
