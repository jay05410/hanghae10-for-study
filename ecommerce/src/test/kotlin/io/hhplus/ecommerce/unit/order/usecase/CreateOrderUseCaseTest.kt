package io.hhplus.ecommerce.unit.order.usecase

import io.hhplus.ecommerce.order.usecase.CreateOrderUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.delivery.application.DeliveryService
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest

class CreateOrderUseCaseTest : DescribeSpec({

    val orderService = mockk<OrderService>()
    val productService = mockk<ProductService>()
    val couponService = mockk<CouponService>()
    val paymentService = mockk<PaymentService>()
    val deliveryService = mockk<DeliveryService>()

    val createOrderUseCase = CreateOrderUseCase(
        orderService = orderService,
        productService = productService,
        couponService = couponService,
        paymentService = paymentService,
        deliveryService = deliveryService
    )

    fun mockDeliveryAddress() = DeliveryAddressRequest(
        recipientName = "김철수",
        phone = "010-1234-5678",
        zipCode = "06234",
        address = "서울시 강남구 테헤란로 123",
        addressDetail = "456호",
        deliveryMessage = "부재 시 문 앞에 놓아주세요"
    )

    describe("CreateOrderUseCase") {

        beforeEach {
            clearAllMocks()
        }

        context("쿠폰을 사용하지 않는 주문 생성 시") {
            it("should create order successfully without coupon") {
                // Given
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(
                            packageTypeId = 1L,
                            packageTypeName = "30일 패키지",
                            packageTypeDays = 30,
                            dailyServing = 2,
                            totalQuantity = 300.0,
                            giftWrap = false,
                            giftMessage = null,
                            quantity = 1,
                            containerPrice = 3000,
                            teaPrice = 7000,
                            giftWrapPrice = 0,
                            teaItems = emptyList()
                        )
                    ),
                    deliveryAddress = mockDeliveryAddress(),
                    usedCouponId = null
                )

                val mockProduct = mockk<Product> {
                    every { id } returns 1L
                    every { pricePer100g } returns 10000
                }

                val mockOrder = mockk<Order> {
                    every { id } returns 1L
                    every { finalAmount } returns 10000L
                }

                every { productService.getProduct(1L) } returns mockProduct
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()
                every { deliveryService.createDelivery(any(), any(), any(), any()) } returns mockk()

                // When
                val result = createOrderUseCase.execute(request)

                // Then
                result shouldBe mockOrder
                verify { productService.getProduct(1L) }
                verify { orderService.createOrder(1L, any(), null, 10000L, 0L, 1L) }
                verify { paymentService.processPayment(1L, 1L, 10000L) }
                verify { deliveryService.createDelivery(1L, any(), any(), 1L) }
                verify(exactly = 0) { couponService.validateCouponUsage(any(), any(), any()) }
            }
        }

        context("쿠폰을 사용하는 주문 생성 시") {
            it("should create order successfully with coupon") {
                // Given
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(
                            packageTypeId = 1L,
                            packageTypeName = "30일 패키지",
                            packageTypeDays = 30,
                            dailyServing = 2,
                            totalQuantity = 300.0,
                            giftWrap = false,
                            giftMessage = null,
                            quantity = 1,
                            containerPrice = 3000,
                            teaPrice = 7000,
                            giftWrapPrice = 0,
                            teaItems = emptyList()
                        )
                    ),
                    deliveryAddress = mockDeliveryAddress(),
                    usedCouponId = 100L
                )

                val mockProduct = mockk<Product> {
                    every { id } returns 1L
                    every { pricePer100g } returns 10000
                }

                val mockOrder = mockk<Order> {
                    every { id } returns 1L
                    every { finalAmount } returns 8000L
                }

                every { productService.getProduct(1L) } returns mockProduct
                every { couponService.validateCouponUsage(1L, 100L, 10000L) } returns 2000L
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()
                every { couponService.applyCoupon(any(), any(), any(), any()) } returns 0L
                every { deliveryService.createDelivery(any(), any(), any(), any()) } returns mockk()

                // When
                val result = createOrderUseCase.execute(request)

                // Then
                result shouldBe mockOrder
                verify { productService.getProduct(1L) }
                verify { couponService.validateCouponUsage(1L, 100L, 10000L) }
                verify { orderService.createOrder(1L, any(), 100L, 10000L, 2000L, 1L) }
                verify { paymentService.processPayment(1L, 1L, 8000L) }
                verify { couponService.applyCoupon(1L, 100L, 1L, 10000L) }
                verify { deliveryService.createDelivery(1L, any(), any(), 1L) }
            }
        }

        context("여러 상품으로 주문 생성 시") {
            it("should create order with multiple items") {
                // Given
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(
                            packageTypeId = 1L,
                            packageTypeName = "30일 패키지",
                            packageTypeDays = 30,
                            dailyServing = 2,
                            totalQuantity = 300.0,
                            giftWrap = false,
                            giftMessage = null,
                            quantity = 1,
                            containerPrice = 3000,
                            teaPrice = 7000,
                            giftWrapPrice = 0,
                            teaItems = emptyList()
                        ),
                        CreateOrderItemRequest(
                            packageTypeId = 2L,
                            packageTypeName = "15일 패키지",
                            packageTypeDays = 15,
                            dailyServing = 1,
                            totalQuantity = 150.0,
                            giftWrap = false,
                            giftMessage = null,
                            quantity = 1,
                            containerPrice = 2000,
                            teaPrice = 3000,
                            giftWrapPrice = 0,
                            teaItems = emptyList()
                        )
                    ),
                    deliveryAddress = mockDeliveryAddress(),
                    usedCouponId = null
                )

                val mockProduct1 = mockk<Product> {
                    every { id } returns 1L
                    every { pricePer100g } returns 10000
                }

                val mockProduct2 = mockk<Product> {
                    every { id } returns 2L
                    every { pricePer100g } returns 15000
                }

                val mockOrder = mockk<Order> {
                    every { id } returns 1L
                    every { finalAmount } returns 15000L
                }

                every { productService.getProduct(1L) } returns mockProduct1
                every { productService.getProduct(2L) } returns mockProduct2
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()
                every { deliveryService.createDelivery(any(), any(), any(), any()) } returns mockk()

                // When
                val result = createOrderUseCase.execute(request)

                // Then
                result shouldBe mockOrder
                verify { productService.getProduct(1L) }
                verify { productService.getProduct(2L) }
                verify { orderService.createOrder(1L, any(), null, 15000L, 0L, 1L) }
                verify { paymentService.processPayment(1L, 1L, 15000L) }
                verify { deliveryService.createDelivery(1L, any(), any(), 1L) }
            }
        }

        context("주문 생성 중 결제 서비스 호출 시") {
            it("should call payment service with correct parameters") {
                // Given
                val request = CreateOrderRequest(
                    userId = 2L,
                    items = listOf(
                        CreateOrderItemRequest(
                            packageTypeId = 1L,
                            packageTypeName = "30일 패키지",
                            packageTypeDays = 30,
                            dailyServing = 1,
                            totalQuantity = 300.0,
                            giftWrap = false,
                            giftMessage = null,
                            quantity = 1,
                            containerPrice = 2000,
                            teaPrice = 3000,
                            giftWrapPrice = 0,
                            teaItems = emptyList()
                        )
                    ),
                    deliveryAddress = mockDeliveryAddress(),
                    usedCouponId = null
                )

                val mockProduct = mockk<Product> {
                    every { id } returns 1L
                    every { pricePer100g } returns 5000
                }

                val mockOrder = mockk<Order> {
                    every { id } returns 10L
                    every { finalAmount } returns 5000L
                }

                every { productService.getProduct(1L) } returns mockProduct
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()
                every { deliveryService.createDelivery(any(), any(), any(), any()) } returns mockk()

                // When
                createOrderUseCase.execute(request)

                // Then
                verify { paymentService.processPayment(2L, 10L, 5000L) }
                verify { deliveryService.createDelivery(10L, any(), any(), 2L) }
            }
        }
    }
})
