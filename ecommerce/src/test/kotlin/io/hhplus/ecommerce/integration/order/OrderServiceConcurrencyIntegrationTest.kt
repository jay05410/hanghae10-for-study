package io.hhplus.ecommerce.integration.order

import io.hhplus.ecommerce.order.application.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.presentation.dto.DeliveryAddressRequest
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.product.application.usecase.ProductCommandUseCase
import io.hhplus.ecommerce.product.presentation.dto.CreateProductRequest
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.user.application.usecase.UserCommandUseCase
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldNotBeEmpty
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * 주문 동시성 통합 테스트 (간소화 버전)
 */
class OrderServiceConcurrencyIntegrationTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val productCommandUseCase: ProductCommandUseCase,
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val userCommandUseCase: UserCommandUseCase,
    private val chargePointUseCase: ChargePointUseCase
) : KotestIntegrationTestBase({

    // 테스트용 데이터
    lateinit var testProduct: Product

    beforeEach {
        // 테스트용 상품 생성
        testProduct = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "동시성 테스트 상품",
                description = "동시성 테스트용 상품",
                price = 10000L,
                categoryId = 1L,
                createdBy = 1L
            )
        )

        // 재고 생성 (충분한 수량)
        inventoryCommandUseCase.createInventory(testProduct.id, 10000)

        // 테스트용 사용자들 생성 및 포인트 충전
        repeat(100) { index ->
            val userId = 1000L + index
            try {
                userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "test$userId@example.com",
                    password = "password",
                    email = "test$userId@example.com",
                    name = "테스트사용자$userId",
                    phone = "010-0000-${String.format("%04d", index)}",
                    providerId = null
                )
                chargePointUseCase.execute(userId, 100000)
            } catch (e: Exception) {
                // 사용자가 이미 존재할 경우 무시하지만 포인트는 충전
                try {
                    chargePointUseCase.execute(userId, 100000)
                } catch (pointE: Exception) {
                    // 포인트 충전도 실패하면 무시
                }
            }
        }
    }

    describe("동시성 주문 서비스") {
        context("동시에 여러 주문을 생성할 때") {
            it("모든 주문이 정상 생성되어야 한다") {
                // Given
                val threadCount = 10
                val executor = Executors.newFixedThreadPool(threadCount)
                val futures = mutableListOf<CompletableFuture<String>>()

                val orderItem = CreateOrderItemRequest(
                    productId = testProduct.id,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                )

                // When: 동시에 여러 주문 직접 처리
                repeat(threadCount) { index ->
                    val future = CompletableFuture.supplyAsync({
                        val userId = 1000L + index
                        val createOrderRequest = CreateOrderRequest(
                            userId = userId,
                            items = listOf(orderItem),
                            usedCouponId = null,
                            deliveryAddress = DeliveryAddressRequest(
                                recipientName = "테스트 수령인",
                                phone = "010-1234-5678",
                                zipCode = "12345",
                                address = "서울시 강남구",
                                addressDetail = "테스트 상세주소",
                                deliveryMessage = "동시성 테스트용 배송"
                            )
                        )
                        val order = orderCommandUseCase.createOrder(createOrderRequest)
                        order.orderNumber
                    }, executor)
                    futures.add(future)
                }

                // 모든 작업 완료 대기
                val orderNumbers = futures.map { it.get() }

                // Then: 주문 생성 검증
                orderNumbers shouldHaveSize threadCount
                orderNumbers.distinct() shouldHaveSize threadCount // 모든 주문 번호가 유니크해야 함
                orderNumbers.all { it.isNotEmpty() } shouldBe true

                executor.shutdown()
            }
        }
    }
})