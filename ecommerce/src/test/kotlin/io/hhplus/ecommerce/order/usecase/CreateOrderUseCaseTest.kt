package io.hhplus.ecommerce.order.usecase

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

class CreateOrderUseCaseTest : DescribeSpec({

    val orderService = mockk<OrderService>()
    val productService = mockk<ProductService>()
    val couponService = mockk<CouponService>()
    val paymentService = mockk<PaymentService>()

    val createOrderUseCase = CreateOrderUseCase(
        orderService = orderService,
        productService = productService,
        couponService = couponService,
        paymentService = paymentService
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
                            productId = 1L,
                            boxTypeId = 1L,
                            quantity = 2,
                            teaItems = emptyList()
                        )
                    ),
                    usedCouponId = null
                )

                val mockProduct = mockk<Product> {
                    every { id } returns 1L
                    every { pricePer100g } returns 10000
                }

                val mockOrder = mockk<Order> {
                    every { id } returns 1L
                    every { finalAmount } returns 20000L
                }

                every { productService.getProduct(1L) } returns mockProduct
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()

                // When
                val result = createOrderUseCase.execute(request)

                // Then
                result shouldBe mockOrder
                verify { productService.getProduct(1L) }
                verify { orderService.createOrder(1L, any(), null, 20000L, 0L, 1L) }
                verify { paymentService.processPayment(1L, 1L, 20000L) }
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
                            productId = 1L,
                            boxTypeId = 1L,
                            quantity = 2,
                            teaItems = emptyList()
                        )
                    ),
                    usedCouponId = 100L
                )

                val mockProduct = mockk<Product> {
                    every { id } returns 1L
                    every { pricePer100g } returns 10000
                }

                val mockOrder = mockk<Order> {
                    every { id } returns 1L
                    every { finalAmount } returns 18000L
                }

                every { productService.getProduct(1L) } returns mockProduct
                every { couponService.validateCouponUsage(1L, 100L, 20000L) } returns 2000L
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()
                every { couponService.applyCoupon(any(), any(), any(), any()) } returns 0L

                // When
                val result = createOrderUseCase.execute(request)

                // Then
                result shouldBe mockOrder
                verify { productService.getProduct(1L) }
                verify { couponService.validateCouponUsage(1L, 100L, 20000L) }
                verify { orderService.createOrder(1L, any(), 100L, 20000L, 2000L, 1L) }
                verify { paymentService.processPayment(1L, 1L, 18000L) }
                verify { couponService.applyCoupon(1L, 100L, 1L, 20000L) }
            }
        }

        context("여러 상품으로 주문 생성 시") {
            it("should create order with multiple items") {
                // Given
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(
                            productId = 1L,
                            boxTypeId = 1L,
                            quantity = 2,
                            teaItems = emptyList()
                        ),
                        CreateOrderItemRequest(
                            productId = 2L,
                            boxTypeId = 1L,
                            quantity = 1,
                            teaItems = emptyList()
                        )
                    ),
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
                    every { finalAmount } returns 35000L
                }

                every { productService.getProduct(1L) } returns mockProduct1
                every { productService.getProduct(2L) } returns mockProduct2
                every { orderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { paymentService.processPayment(any(), any(), any()) } returns mockk<io.hhplus.ecommerce.payment.domain.entity.Payment>()

                // When
                val result = createOrderUseCase.execute(request)

                // Then
                result shouldBe mockOrder
                verify { productService.getProduct(1L) }
                verify { productService.getProduct(2L) }
                verify { orderService.createOrder(1L, any(), null, 35000L, 0L, 1L) }
                verify { paymentService.processPayment(1L, 1L, 35000L) }
            }
        }

        context("주문 생성 중 결제 서비스 호출 시") {
            it("should call payment service with correct parameters") {
                // Given
                val request = CreateOrderRequest(
                    userId = 2L,
                    items = listOf(
                        CreateOrderItemRequest(
                            productId = 1L,
                            boxTypeId = 1L,
                            quantity = 1,
                            teaItems = emptyList()
                        )
                    ),
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

                // When
                createOrderUseCase.execute(request)

                // Then
                verify { paymentService.processPayment(2L, 10L, 5000L) }
            }
        }
    }
})