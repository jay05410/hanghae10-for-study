package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * IssueCouponUseCase 단위 테스트
 *
 * 책임: 쿠폰 발급 비즈니스 흐름 검증
 * - 쿠폰 발급 로직의 서비스 위임 검증
 * - 파라미터 전달 및 결과 반환 검증
 *
 * 검증 목표:
 * 1. CouponService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 다양한 요청 데이터에 대한 처리가 올바른가?
 */
class IssueCouponUseCaseTest : DescribeSpec({
    val mockCouponService = mockk<CouponService>()
    val sut = IssueCouponUseCase(mockCouponService)

    beforeEach {
        clearMocks(mockCouponService)
    }

    describe("execute") {
        context("정상적인 쿠폰 발급 요청") {
            it("CouponService에 발급을 위임하고 발급된 사용자 쿠폰을 반환") {
                val userId = 1L
                val request = IssueCouponRequest(couponId = 1L)
                val expectedUserCoupon = mockk<UserCoupon>()

                every { mockCouponService.issueCoupon(userId, request.couponId) } returns expectedUserCoupon

                val result = sut.execute(userId, request)

                result shouldBe expectedUserCoupon
                verify(exactly = 1) { mockCouponService.issueCoupon(userId, request.couponId) }
            }
        }

        context("다른 사용자의 쿠폰 발급 요청") {
            it("각각의 사용자와 쿠폰에 대해 정확한 파라미터 전달") {
                val userId = 2L
                val request = IssueCouponRequest(couponId = 5L)
                val expectedUserCoupon = mockk<UserCoupon>()

                every { mockCouponService.issueCoupon(userId, request.couponId) } returns expectedUserCoupon

                val result = sut.execute(userId, request)

                result shouldBe expectedUserCoupon
                verify(exactly = 1) { mockCouponService.issueCoupon(userId, request.couponId) }
            }
        }

        context("다양한 쿠폰 ID로 발급 요청") {
            it("모든 쿠폰 ID가 정확히 서비스에 전달되는지 확인") {
                val userId = 1L
                val couponIds = listOf(1L, 100L, 999L)

                couponIds.forEach { couponId ->
                    val request = IssueCouponRequest(couponId = couponId)
                    val expectedUserCoupon = mockk<UserCoupon>()

                    every { mockCouponService.issueCoupon(userId, couponId) } returns expectedUserCoupon

                    val result = sut.execute(userId, request)

                    result shouldBe expectedUserCoupon
                    verify(exactly = 1) { mockCouponService.issueCoupon(userId, couponId) }
                    clearMocks(mockCouponService)
                }
            }
        }

        context("다양한 사용자와 쿠폰 조합") {
            it("모든 파라미터가 정확히 서비스에 전달되는지 확인") {
                val testCases = listOf(
                    Pair(1L, 1L),
                    Pair(100L, 200L),
                    Pair(999L, 888L)
                )

                testCases.forEach { (userId, couponId) ->
                    val request = IssueCouponRequest(couponId = couponId)
                    val expectedUserCoupon = mockk<UserCoupon>()

                    every { mockCouponService.issueCoupon(userId, couponId) } returns expectedUserCoupon

                    val result = sut.execute(userId, request)

                    result shouldBe expectedUserCoupon
                    verify(exactly = 1) { mockCouponService.issueCoupon(userId, couponId) }
                    clearMocks(mockCouponService)
                }
            }
        }

        context("연속된 발급 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val request1 = IssueCouponRequest(couponId = 1L)
                val request2 = IssueCouponRequest(couponId = 2L)
                val expectedUserCoupon1 = mockk<UserCoupon>()
                val expectedUserCoupon2 = mockk<UserCoupon>()

                every { mockCouponService.issueCoupon(userId, 1L) } returns expectedUserCoupon1
                every { mockCouponService.issueCoupon(userId, 2L) } returns expectedUserCoupon2

                val result1 = sut.execute(userId, request1)
                val result2 = sut.execute(userId, request2)

                result1 shouldBe expectedUserCoupon1
                result2 shouldBe expectedUserCoupon2
                verify(exactly = 1) { mockCouponService.issueCoupon(userId, 1L) }
                verify(exactly = 1) { mockCouponService.issueCoupon(userId, 2L) }
            }
        }
    }
})