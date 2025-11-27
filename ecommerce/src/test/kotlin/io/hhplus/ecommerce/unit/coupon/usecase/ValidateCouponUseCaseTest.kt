package io.hhplus.ecommerce.unit.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.usecase.CouponUseCase
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CouponUseCase 단위 테스트 (검증 기능)
 *
 * 책임: 쿠폰 사용 검증 비즈니스 흐름 검증
 * - 쿠폰 사용 가능성 검증 로직의 서비스 위임 검증
 * - 할인 금액 계산 결과 반환 검증
 *
 * 검증 목표:
 * 1. CouponService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 다양한 요청 데이터에 대한 처리가 올바른가?
 */
class CouponUseCaseValidateTest : DescribeSpec({
    val mockCouponService = mockk<CouponService>()
    val sut = CouponUseCase(mockCouponService, mockk(), mockk())

    beforeEach {
        clearMocks(mockCouponService)
    }

    describe("execute") {
        context("정상적인 쿠폰 검증 요청") {
            it("CouponService에 검증을 위임하고 할인 금액을 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 50000L
                )
                val expectedDiscountAmount = 5000L

                every { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) }
            }
        }

        context("다른 사용자의 쿠폰 검증 요청") {
            it("각각의 사용자와 쿠폰에 대해 정확한 파라미터 전달") {
                val userId = 2L
                val request = UseCouponRequest(
                    userCouponId = 5L,
                    orderAmount = 100000L
                )
                val expectedDiscountAmount = 10000L

                every { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) }
            }
        }

        context("다양한 주문 금액으로 검증 요청") {
            it("모든 주문 금액이 정확히 서비스에 전달되는지 확인") {
                val userId = 1L
                val userCouponId = 1L
                val orderAmounts = listOf(10000L, 50000L, 100000L)
                val discountAmounts = listOf(1000L, 5000L, 10000L)

                orderAmounts.forEachIndexed { index, orderAmount ->
                    val request = UseCouponRequest(
                        userCouponId = userCouponId,
                        orderAmount = orderAmount
                    )

                    every { mockCouponService.validateCouponUsage(userId, userCouponId, orderAmount) } returns discountAmounts[index]

                    val result = sut.validateCoupon(userId, request)

                    result shouldBe discountAmounts[index]
                    verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, userCouponId, orderAmount) }
                    clearMocks(mockCouponService)
                }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 서비스에 전달되는지 확인") {
                val testCases = listOf(
                    Triple(1L, 1L, 30000L),
                    Triple(100L, 200L, 50000L),
                    Triple(999L, 888L, 100000L)
                )

                testCases.forEachIndexed { index, (userId, userCouponId, orderAmount) ->
                    val request = UseCouponRequest(
                        userCouponId = userCouponId,
                        orderAmount = orderAmount
                    )
                    val expectedDiscountAmount = (index + 1) * 1000L

                    every { mockCouponService.validateCouponUsage(userId, userCouponId, orderAmount) } returns expectedDiscountAmount

                    val result = sut.validateCoupon(userId, request)

                    result shouldBe expectedDiscountAmount
                    verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, userCouponId, orderAmount) }
                    clearMocks(mockCouponService)
                }
            }
        }

        context("할인 금액이 0인 경우") {
            it("0을 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 5000L  // 최소 주문 금액 미달
                )
                val expectedDiscountAmount = 0L

                every { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) }
            }
        }

        context("큰 주문 금액의 경우") {
            it("큰 할인 금액도 정확히 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 1000000L
                )
                val expectedDiscountAmount = 100000L

                every { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, request.userCouponId, request.orderAmount) }
            }
        }

        context("연속된 검증 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val request1 = UseCouponRequest(userCouponId = 1L, orderAmount = 30000L)
                val request2 = UseCouponRequest(userCouponId = 2L, orderAmount = 60000L)
                val expectedDiscountAmount1 = 3000L
                val expectedDiscountAmount2 = 6000L

                every { mockCouponService.validateCouponUsage(userId, 1L, 30000L) } returns expectedDiscountAmount1
                every { mockCouponService.validateCouponUsage(userId, 2L, 60000L) } returns expectedDiscountAmount2

                val result1 = sut.validateCoupon(userId, request1)
                val result2 = sut.validateCoupon(userId, request2)

                result1 shouldBe expectedDiscountAmount1
                result2 shouldBe expectedDiscountAmount2
                verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, 1L, 30000L) }
                verify(exactly = 1) { mockCouponService.validateCouponUsage(userId, 2L, 60000L) }
            }
        }
    }
})