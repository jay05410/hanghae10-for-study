package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.payment.application.PaymentService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * CreateOrderUseCase 단위 테스트
 *
 * 책임: 주문 생성 유스케이스의 비즈니스 플로우 검증
 * - 주문 생성 전 단계 비즈니스 검증
 * - 여러 도메인 서비스 조합 및 트랜잭션 관리
 */
class CreateOrderUseCaseTest : DescribeSpec({
    val mockOrderService = mockk<OrderService>()
    val mockProductService = mockk<ProductService>()
    val mockCouponService = mockk<CouponService>()
    val mockPaymentService = mockk<PaymentService>()

    val sut = CreateOrderUseCase(
        orderService = mockOrderService,
        productService = mockProductService,
        couponService = mockCouponService,
        paymentService = mockPaymentService
    )

    fun createMockOrder(
        id: Long = 1L,
        userId: Long = 1L,
        finalAmount: Long = 10000L
    ): Order = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.userId } returns userId
        every { this@mockk.finalAmount } returns finalAmount
        every { status } returns OrderStatus.PENDING
    }

    fun createMockProduct(
        id: Long = 1L,
        pricePer100g: Int = 5000
    ): Product = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.pricePer100g } returns pricePer100g
    }

    beforeEach {
        clearMocks(
            mockOrderService,
            mockProductService,
            mockCouponService,
            mockPaymentService
        )
    }

    describe("execute") {
        context("쿠폰 없는 정상적인 주문 생성") {
            it("상품 검증, 주문 생성, 결제 처리를 순차적으로 수행") {
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(productId = 1L, boxTypeId = 1L, quantity = 2)
                    ),
                    usedCouponId = null
                )
                val mockProduct = createMockProduct(id = 1L, pricePer100g = 5000)
                val mockOrder = createMockOrder(id = 1L, userId = 1L, finalAmount = 10000L)

                every { mockProductService.getProduct(1L) } returns mockProduct
                every { mockOrderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { mockPaymentService.processPayment(any(), any(), any()) } returns mockk()

                val result = sut.execute(request)

                result shouldBe mockOrder
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockOrderService.createOrder(any(), any(), null, 10000L, 0L, 1L) }
                verify(exactly = 1) { mockPaymentService.processPayment(1L, 1L, 10000L) }
                verify(exactly = 0) { mockCouponService.validateCouponUsage(any(), any(), any()) }
            }
        }

        context("쿠폰이 있는 주문 생성") {
            it("쿠폰 검증 및 적용을 포함하여 주문을 처리") {
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(productId = 1L, boxTypeId = 1L, quantity = 2)
                    ),
                    usedCouponId = 100L
                )
                val mockProduct = createMockProduct(id = 1L, pricePer100g = 5000)
                val mockOrder = createMockOrder(id = 1L, userId = 1L, finalAmount = 8000L)

                every { mockProductService.getProduct(1L) } returns mockProduct
                every { mockCouponService.validateCouponUsage(1L, 100L, 10000L) } returns 2000L
                every { mockOrderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { mockPaymentService.processPayment(any(), any(), any()) } returns mockk()
                every { mockCouponService.applyCoupon(any(), any(), any(), any()) } returns mockk()

                val result = sut.execute(request)

                result shouldBe mockOrder
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockCouponService.validateCouponUsage(1L, 100L, 10000L) }
                verify(exactly = 1) { mockOrderService.createOrder(any(), any(), 100L, 10000L, 2000L, 1L) }
                verify(exactly = 1) { mockPaymentService.processPayment(1L, 1L, 8000L) }
                verify(exactly = 1) { mockCouponService.applyCoupon(1L, 100L, 1L, 10000L) }
            }
        }

        context("여러 상품으로 주문 생성") {
            it("모든 상품을 검증하고 총 금액을 계산") {
                val request = CreateOrderRequest(
                    userId = 1L,
                    items = listOf(
                        CreateOrderItemRequest(productId = 1L, boxTypeId = 1L, quantity = 2),
                        CreateOrderItemRequest(productId = 2L, boxTypeId = 2L, quantity = 1)
                    ),
                    usedCouponId = null
                )
                val mockProduct1 = createMockProduct(id = 1L, pricePer100g = 5000)
                val mockProduct2 = createMockProduct(id = 2L, pricePer100g = 3000)
                val mockOrder = createMockOrder(id = 1L, userId = 1L, finalAmount = 13000L)

                every { mockProductService.getProduct(1L) } returns mockProduct1
                every { mockProductService.getProduct(2L) } returns mockProduct2
                every { mockOrderService.createOrder(any(), any(), any(), any(), any(), any()) } returns mockOrder
                every { mockPaymentService.processPayment(any(), any(), any()) } returns mockk()

                val result = sut.execute(request)

                result shouldBe mockOrder
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
                verify(exactly = 1) { mockOrderService.createOrder(any(), any(), null, 13000L, 0L, 1L) }
                verify(exactly = 1) { mockPaymentService.processPayment(1L, 1L, 13000L) }
            }
        }
    }
})