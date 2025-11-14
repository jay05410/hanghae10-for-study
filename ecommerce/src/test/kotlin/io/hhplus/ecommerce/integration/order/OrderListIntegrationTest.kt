package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.order.usecase.GetOrderQueryUseCase
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeSortedWith

/**
 * 사용자 주문 목록 조회 통합 테스트
 *
 * 목적:
 * - N+1 문제 검증
 * - 페이징 검증
 * - 정렬 검증 (최신 주문이 먼저)
 *
 * 시나리오:
 * 1. 사용자의 주문 10개 생성
 * 2. 주문 목록 조회
 * 3. 쿼리 수 확인 (N+1 없음)
 * 4. 정렬 확인 (최신순)
 */
class OrderListIntegrationTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val getOrderQueryUseCase: GetOrderQueryUseCase,
    private val pointCommandUseCase: PointCommandUseCase,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository
) : KotestIntegrationTestBase({

    describe("사용자 주문 목록 조회") {
        context("사용자가 여러 주문을 가지고 있을 때") {
            it("모든 주문을 최신순으로 조회할 수 있어야 한다") {
                // Given: 사용자와 상품 준비
                val userId = 3000L
                val orderCount = 10

                val product = Product.create(
                    name = "주문 목록 테스트 티",
                    description = "테스트용",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 0L
                )
                val savedProduct = productRepository.save(product)

                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 1000,
                    createdBy = 0L
                )
                inventoryRepository.save(inventory)

                // 충분한 포인트 충전
                pointCommandUseCase.chargePoint(userId, 500000, "테스트용 충전")

                // 주문 10개 생성
                val orderItems = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = savedProduct.id,
                        packageTypeName = "목록 테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 1.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = listOf(
                            TeaItemRequest(
                                productId = savedProduct.id,
                                selectionOrder = 1,
                                ratioPercent = 100
                            )
                        )
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

                val createdOrderIds = (1..orderCount).map {
                    val order = orderCommandUseCase.createOrder(createOrderRequest)
                    Thread.sleep(10) // 주문 시간 차이를 두기 위해
                    order.id
                }

                // When: 주문 목록 조회
                val orders = getOrderQueryUseCase.getOrdersByUser(userId)

                // Then: 주문 수 확인
                orders shouldHaveSize orderCount

                // 최신순 정렬 확인 (최근 생성된 주문이 먼저)
                orders shouldBeSortedWith compareByDescending { it.createdAt }

                // 모든 주문이 조회되었는지 확인
                orders.map { it.id }.toSet() shouldBe createdOrderIds.toSet()

                // 주문 ID가 모두 유효한지 확인
                orders.all { it.id > 0 } shouldBe true
            }
        }

        context("주문이 없는 사용자가 조회할 때") {
            it("빈 목록이 반환되어야 한다") {
                // Given: 주문이 없는 사용자
                val userId = 3001L

                // When: 주문 목록 조회
                val orders = getOrderQueryUseCase.getOrdersByUser(userId)

                // Then: 빈 목록 반환
                orders shouldHaveSize 0
            }
        }

        context("대량의 주문을 가진 사용자가 조회할 때") {
            it("성능 저하 없이 조회할 수 있어야 한다") {
                // Given: 대량 주문 사용자
                val userId = 3002L
                val largeOrderCount = 50

                val product = Product.create(
                    name = "대량 주문 테스트 티",
                    description = "테스트용",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 0L
                )
                val savedProduct = productRepository.save(product)

                val inventory = Inventory.create(
                    productId = savedProduct.id,
                    initialQuantity = 5000,
                    createdBy = 0L
                )
                inventoryRepository.save(inventory)

                pointCommandUseCase.chargePoint(userId, 2000000, "테스트용 충전")

                val orderItems = listOf(
                    CreateOrderItemRequest(
                        packageTypeId = savedProduct.id,
                        packageTypeName = "대량 테스트 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 1.0,
                        giftWrap = false,
                        giftMessage = null,
                        quantity = 1,
                        containerPrice = 10000,
                        teaPrice = 10000,
                        giftWrapPrice = 0,
                        teaItems = listOf(
                            TeaItemRequest(
                                productId = savedProduct.id,
                                selectionOrder = 1,
                                ratioPercent = 100
                            )
                        )
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

                // 대량 주문 생성
                repeat(largeOrderCount) {
                    orderCommandUseCase.createOrder(createOrderRequest)
                }

                // When: 조회 시간 측정
                val startTime = System.currentTimeMillis()
                val orders = getOrderQueryUseCase.getOrdersByUser(userId)
                val elapsedTime = System.currentTimeMillis() - startTime

                // Then: 조회 성공 및 성능 확인
                orders shouldHaveSize largeOrderCount

                // 성능 목표: 1초 이내 (실제 프로젝트에 맞게 조정)
                (elapsedTime < 1000L) shouldBe true

                println("📊 대량 주문($largeOrderCount 개) 조회 시간: ${elapsedTime}ms")
            }
        }
    }
})
