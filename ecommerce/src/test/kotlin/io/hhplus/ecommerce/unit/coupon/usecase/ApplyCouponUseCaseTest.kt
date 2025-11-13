package io.hhplus.ecommerce.unit.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ApplyCouponUseCase 단위 테스트
 *
 * 책임: 쿠폰 사용 비즈니스 흐름 검증
 * - 실제 쿠폰 사용 처리 로직의 서비스 위임 검증
 * - 할인 금액 계산 결과 반환 검증
 *
 * 검증 목표:
 * 1. CouponService에 올바른 파라미터가 전달되는가?
 * 2. orderId가 별도로 전달되어 적절히 처리되는가?
 * 3. 서비스 결과가 그대로 반환되는가?
 * 4. 다양한 요청 데이터에 대한 처리가 올바른가?
 */
class ApplyCouponUseCaseTest : DescribeSpec({
    val mockCouponService = mockk<CouponService>()
    val sut = ApplyCouponUseCase(mockCouponService)

    beforeEach {
        clearMocks(mockCouponService)
    }

    describe("execute") {
        context("정상적인 쿠폰 사용 요청") {
            it("CouponService에 사용을 위임하고 할인 금액을 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 50000L
                )
                val orderId = 1L
                val expectedDiscountAmount = 5000L

                every { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.execute(userId, request, orderId)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) }
            }
        }

        context("다른 사용자의 쿠폰 사용 요청") {
            it("각각의 사용자, 쿠폰, 주문에 대해 정확한 파라미터 전달") {
                val userId = 2L
                val request = UseCouponRequest(
                    userCouponId = 5L,
                    orderAmount = 100000L
                )
                val orderId = 2L
                val expectedDiscountAmount = 10000L

                every { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.execute(userId, request, orderId)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) }
            }
        }

        context("다양한 주문 ID로 사용 요청") {
            it("모든 주문 ID가 정확히 서비스에 전달되는지 확인") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 50000L
                )
                val orderIds = listOf(1L, 100L, 999L)
                val expectedDiscountAmount = 5000L

                orderIds.forEach { orderId ->
                    every { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) } returns expectedDiscountAmount

                    val result = sut.execute(userId, request, orderId)

                    result shouldBe expectedDiscountAmount
                    verify(exactly = 1) { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) }
                    clearMocks(mockCouponService)
                }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 서비스에 전달되는지 확인") {
                data class TestCase(
                    val userId: Long,
                    val userCouponId: Long,
                    val orderId: Long,
                    val orderAmount: Long,
                    val expectedDiscount: Long
                )

                val testCases = listOf(
                    TestCase(1L, 1L, 1L, 30000L, 3000L),
                    TestCase(100L, 200L, 300L, 50000L, 5000L),
                    TestCase(999L, 888L, 777L, 100000L, 10000L)
                )

                testCases.forEach { testCase ->
                    val request = UseCouponRequest(
                        userCouponId = testCase.userCouponId,
                        orderAmount = testCase.orderAmount
                    )

                    every { mockCouponService.applyCoupon(testCase.userId, testCase.userCouponId, testCase.orderId, testCase.orderAmount) } returns testCase.expectedDiscount

                    val result = sut.execute(testCase.userId, request, testCase.orderId)

                    result shouldBe testCase.expectedDiscount
                    verify(exactly = 1) { mockCouponService.applyCoupon(testCase.userId, testCase.userCouponId, testCase.orderId, testCase.orderAmount) }
                    clearMocks(mockCouponService)
                }
            }
        }

        context("할인 금액이 0인 경우") {
            it("0을 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 5000L  // 최소 주문 금액 미달로 할인 불가
                )
                val orderId = 1L
                val expectedDiscountAmount = 0L

                every { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.execute(userId, request, orderId)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) }
            }
        }

        context("큰 주문 금액의 경우") {
            it("큰 할인 금액도 정확히 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 1000000L
                )
                val orderId = 1L
                val expectedDiscountAmount = 100000L

                every { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) } returns expectedDiscountAmount

                val result = sut.execute(userId, request, orderId)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.applyCoupon(userId, request.userCouponId, orderId, request.orderAmount) }
            }
        }

        context("연속된 사용 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val request1 = UseCouponRequest(userCouponId = 1L, orderAmount = 30000L)
                val request2 = UseCouponRequest(userCouponId = 2L, orderAmount = 60000L)
                val orderId1 = 1L
                val orderId2 = 2L
                val expectedDiscountAmount1 = 3000L
                val expectedDiscountAmount2 = 6000L

                every { mockCouponService.applyCoupon(userId, 1L, orderId1, 30000L) } returns expectedDiscountAmount1
                every { mockCouponService.applyCoupon(userId, 2L, orderId2, 60000L) } returns expectedDiscountAmount2

                val result1 = sut.execute(userId, request1, orderId1)
                val result2 = sut.execute(userId, request2, orderId2)

                result1 shouldBe expectedDiscountAmount1
                result2 shouldBe expectedDiscountAmount2
                verify(exactly = 1) { mockCouponService.applyCoupon(userId, 1L, orderId1, 30000L) }
                verify(exactly = 1) { mockCouponService.applyCoupon(userId, 2L, orderId2, 60000L) }
            }
        }

        context("파라미터 순서 검증") {
            it("userId, userCouponId, orderId, orderAmount 순서로 정확히 전달") {
                val userId = 42L
                val userCouponId = 24L
                val orderId = 12L
                val orderAmount = 84000L
                val request = UseCouponRequest(
                    userCouponId = userCouponId,
                    orderAmount = orderAmount
                )
                val expectedDiscountAmount = 8400L

                every { mockCouponService.applyCoupon(userId, userCouponId, orderId, orderAmount) } returns expectedDiscountAmount

                val result = sut.execute(userId, request, orderId)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponService.applyCoupon(
                    userId,           // 첫 번째 파라미터
                    userCouponId,    // 두 번째 파라미터 (request에서 추출)
                    orderId,         // 세 번째 파라미터 (별도 전달)
                    orderAmount      // 네 번째 파라미터 (request에서 추출)
                ) }
            }
        }
    }
})