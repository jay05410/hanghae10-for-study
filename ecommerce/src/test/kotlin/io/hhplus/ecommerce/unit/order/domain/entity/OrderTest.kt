package io.hhplus.ecommerce.unit.order.domain.entity

import io.hhplus.ecommerce.order.domain.entity.Order
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrow
import io.hhplus.ecommerce.order.domain.constant.OrderStatus

class OrderTest : DescribeSpec({

    describe("Order Entity") {

        context("주문 생성 시") {
            it("should create order with valid parameters") {
                // Given
                val orderNumber = "ORD-20241107-001"
                val userId = 1L
                val totalAmount = 10000L
                val discountAmount = 1000L
                val usedCouponIds = listOf(100L)

                // When
                val order = Order.create(
                    orderNumber = orderNumber,
                    userId = userId,
                    totalAmount = totalAmount,
                    discountAmount = discountAmount,
                    usedCouponIds = usedCouponIds
                )

                // Then
                order shouldNotBe null
                order.orderNumber shouldBe orderNumber
                order.userId shouldBe userId
                order.totalAmount shouldBe totalAmount
                order.discountAmount shouldBe discountAmount
                order.usedCouponIds shouldBe usedCouponIds
                order.status shouldBe OrderStatus.PENDING
                order.finalAmount shouldBe (totalAmount - discountAmount)
            }

            it("should create order without coupon") {
                // Given
                val orderNumber = "ORD-20241107-002"
                val userId = 2L
                val totalAmount = 5000L
                val discountAmount = 0L

                // When
                val order = Order.create(
                    orderNumber = orderNumber,
                    userId = userId,
                    totalAmount = totalAmount,
                    discountAmount = discountAmount,
                    usedCouponIds = emptyList()
                )

                // Then
                order.usedCouponIds shouldBe emptyList()
                order.finalAmount shouldBe totalAmount
            }
        }

        // Note: OrderItem management is now done through OrderItemRepository,
        // not through Order entity itself (immutable pattern)

        context("주문 상태 변경 시") {
            it("should confirm order successfully (mutates in place)") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-005",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponIds = emptyList()
                )

                // When
                order.confirm()

                // Then
                order.status shouldBe OrderStatus.CONFIRMED
            }

            it("should cancel order successfully (mutates in place)") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-006",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponIds = emptyList()
                )

                // When
                order.cancel()

                // Then
                order.status shouldBe OrderStatus.CANCELLED
            }

            it("should complete order successfully (mutates in place)") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-007",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponIds = emptyList()
                )
                order.confirm()

                // When
                order.complete()

                // Then
                order.status shouldBe OrderStatus.COMPLETED
            }
        }

        context("주문 검증 시") {
            it("should validate total amount is positive") {
                // When & Then
                shouldThrow<IllegalArgumentException> {
                    Order.create(
                        orderNumber = "ORD-20241107-008",
                        userId = 1L,
                        totalAmount = -1000L,
                        discountAmount = 0L,
                        usedCouponIds = emptyList()
                    )
                }
            }

            it("should validate discount amount is not negative") {
                // When & Then
                shouldThrow<IllegalArgumentException> {
                    Order.create(
                        orderNumber = "ORD-20241107-009",
                        userId = 1L,
                        totalAmount = 10000L,
                        discountAmount = -500L,
                        usedCouponIds = emptyList()
                    )
                }
            }

            it("should validate discount amount is not greater than total amount") {
                // When & Then
                shouldThrow<IllegalArgumentException> {
                    Order.create(
                        orderNumber = "ORD-20241107-010",
                        userId = 1L,
                        totalAmount = 5000L,
                        discountAmount = 6000L,
                        usedCouponIds = emptyList()
                    )
                }
            }
        }
    }
})