package io.hhplus.ecommerce.order.domain.entity

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
                val usedCouponId = 100L

                // When
                val order = Order.create(
                    orderNumber = orderNumber,
                    userId = userId,
                    totalAmount = totalAmount,
                    discountAmount = discountAmount,
                    usedCouponId = usedCouponId
                )

                // Then
                order shouldNotBe null
                order.orderNumber shouldBe orderNumber
                order.userId shouldBe userId
                order.totalAmount shouldBe totalAmount
                order.discountAmount shouldBe discountAmount
                order.usedCouponId shouldBe usedCouponId
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
                    usedCouponId = null
                )

                // Then
                order.usedCouponId shouldBe null
                order.finalAmount shouldBe totalAmount
            }
        }

        context("주문 아이템 추가 시") {
            it("should add order item successfully") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-003",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponId = null
                )

                // When
                order.addItem(
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
                    giftWrapPrice = 0
                )

                // Then
                order.items shouldHaveSize 1
                val item = order.items.first()
                item.packageTypeId shouldBe 1L
                item.packageTypeName shouldBe "30일 패키지"
                item.packageTypeDays shouldBe 30
                item.dailyServing shouldBe 2
                item.totalQuantity shouldBe 300.0
                item.quantity shouldBe 1
                item.containerPrice shouldBe 3000
                item.teaPrice shouldBe 7000
                item.totalPrice shouldBe 10000
            }

            it("should add multiple order items") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-004",
                    userId = 1L,
                    totalAmount = 15000L,
                    discountAmount = 0L,
                    usedCouponId = null
                )

                // When
                order.addItem(
                    packageTypeId = 1L, packageTypeName = "30일", packageTypeDays = 30,
                    dailyServing = 2, totalQuantity = 300.0, giftWrap = false, giftMessage = null,
                    quantity = 1, containerPrice = 3000, teaPrice = 7000, giftWrapPrice = 0
                )
                order.addItem(
                    packageTypeId = 2L, packageTypeName = "15일", packageTypeDays = 15,
                    dailyServing = 1, totalQuantity = 150.0, giftWrap = false, giftMessage = null,
                    quantity = 1, containerPrice = 2000, teaPrice = 3000, giftWrapPrice = 0
                )

                // Then
                order.items shouldHaveSize 2
            }
        }

        context("주문 상태 변경 시") {
            it("should confirm order successfully") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-005",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponId = null
                )

                // When
                order.confirm(1L)

                // Then
                order.status shouldBe OrderStatus.CONFIRMED
            }

            it("should cancel order successfully") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-006",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponId = null
                )

                // When
                order.cancel(1L)

                // Then
                order.status shouldBe OrderStatus.CANCELLED
            }

            it("should complete order successfully") {
                // Given
                val order = Order.create(
                    orderNumber = "ORD-20241107-007",
                    userId = 1L,
                    totalAmount = 10000L,
                    discountAmount = 0L,
                    usedCouponId = null
                )
                order.confirm(1L)

                // When
                order.complete(1L)

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
                        usedCouponId = null
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
                        usedCouponId = null
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
                        usedCouponId = null
                    )
                }
            }
        }
    }
})