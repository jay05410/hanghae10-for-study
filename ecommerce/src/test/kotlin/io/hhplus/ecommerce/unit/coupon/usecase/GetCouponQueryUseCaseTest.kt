package io.hhplus.ecommerce.unit.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.usecase.GetCouponQueryUseCase
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetCouponQueryUseCase 단위 테스트
 *
 * 책임: 쿠폰 조회 비즈니스 흐름 검증
 * - 다양한 쿠폰 조회 로직의 서비스 위임 검증
 * - 파라미터 전달 및 결과 반환 검증
 *
 * 검증 목표:
 * 1. CouponService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 다양한 조회 메서드가 올바른 서비스 메서드를 호출하는가?
 */
class GetCouponQueryUseCaseTest : DescribeSpec({
    val mockCouponService = mockk<CouponService>()
    val sut = GetCouponQueryUseCase(mockCouponService)

    beforeEach {
        clearMocks(mockCouponService)
    }

    describe("getAvailableCoupons") {
        context("발급 가능한 쿠폰 목록 조회") {
            it("CouponService에 조회를 위임하고 쿠폰 목록을 반환") {
                val expectedCoupons = listOf(
                    mockk<Coupon>(),
                    mockk<Coupon>(),
                    mockk<Coupon>()
                )

                every { mockCouponService.getAvailableCoupons() } returns expectedCoupons

                val result = sut.getAvailableCoupons()

                result shouldBe expectedCoupons
                verify(exactly = 1) { mockCouponService.getAvailableCoupons() }
            }
        }

        context("발급 가능한 쿠폰이 없는 경우") {
            it("빈 리스트를 반환") {
                val emptyCoupons = emptyList<Coupon>()

                every { mockCouponService.getAvailableCoupons() } returns emptyCoupons

                val result = sut.getAvailableCoupons()

                result shouldBe emptyCoupons
                verify(exactly = 1) { mockCouponService.getAvailableCoupons() }
            }
        }
    }

    describe("getUserCoupons") {
        context("사용자의 모든 쿠폰 조회") {
            it("CouponService에 조회를 위임하고 사용자 쿠폰 목록을 반환") {
                val userId = 1L
                val expectedUserCoupons = listOf(
                    mockk<UserCoupon>(),
                    mockk<UserCoupon>()
                )

                every { mockCouponService.getUserCoupons(userId) } returns expectedUserCoupons

                val result = sut.getUserCoupons(userId)

                result shouldBe expectedUserCoupons
                verify(exactly = 1) { mockCouponService.getUserCoupons(userId) }
            }
        }

        context("쿠폰이 없는 사용자 조회") {
            it("빈 리스트를 반환") {
                val userId = 999L
                val emptyUserCoupons = emptyList<UserCoupon>()

                every { mockCouponService.getUserCoupons(userId) } returns emptyUserCoupons

                val result = sut.getUserCoupons(userId)

                result shouldBe emptyUserCoupons
                verify(exactly = 1) { mockCouponService.getUserCoupons(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("각각의 사용자 ID가 정확히 서비스에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L)
                val mockUserCoupons = userIds.map { listOf(mockk<UserCoupon>()) }

                userIds.forEachIndexed { index, userId ->
                    every { mockCouponService.getUserCoupons(userId) } returns mockUserCoupons[index]

                    val result = sut.getUserCoupons(userId)

                    result shouldBe mockUserCoupons[index]
                    verify(exactly = 1) { mockCouponService.getUserCoupons(userId) }
                    clearMocks(mockCouponService)
                }
            }
        }
    }

    describe("getAvailableUserCoupons") {
        context("사용자의 사용 가능한 쿠폰 조회") {
            it("CouponService에 조회를 위임하고 사용 가능한 쿠폰 목록을 반환") {
                val userId = 1L
                val expectedAvailableCoupons = listOf(
                    mockk<UserCoupon>(),
                    mockk<UserCoupon>()
                )

                every { mockCouponService.getAvailableUserCoupons(userId) } returns expectedAvailableCoupons

                val result = sut.getAvailableUserCoupons(userId)

                result shouldBe expectedAvailableCoupons
                verify(exactly = 1) { mockCouponService.getAvailableUserCoupons(userId) }
            }
        }

        context("사용 가능한 쿠폰이 없는 사용자") {
            it("빈 리스트를 반환") {
                val userId = 999L
                val emptyAvailableCoupons = emptyList<UserCoupon>()

                every { mockCouponService.getAvailableUserCoupons(userId) } returns emptyAvailableCoupons

                val result = sut.getAvailableUserCoupons(userId)

                result shouldBe emptyAvailableCoupons
                verify(exactly = 1) { mockCouponService.getAvailableUserCoupons(userId) }
            }
        }

        context("다양한 사용자의 사용 가능한 쿠폰 조회") {
            it("각각의 사용자 ID가 정확히 서비스에 전달되는지 확인") {
                val testCases = listOf(
                    Pair(1L, listOf(mockk<UserCoupon>(), mockk<UserCoupon>())),
                    Pair(2L, listOf(mockk<UserCoupon>())),
                    Pair(3L, emptyList())
                )

                testCases.forEach { (userId, expectedCoupons) ->
                    every { mockCouponService.getAvailableUserCoupons(userId) } returns expectedCoupons

                    val result = sut.getAvailableUserCoupons(userId)

                    result shouldBe expectedCoupons
                    verify(exactly = 1) { mockCouponService.getAvailableUserCoupons(userId) }
                    clearMocks(mockCouponService)
                }
            }
        }
    }

    describe("모든 조회 메서드 독립성 검증") {
        context("각 조회 메서드") {
            it("해당하는 서비스 메서드만 호출하고 다른 메서드는 호출하지 않음") {
                val userId = 1L

                // getAvailableCoupons 테스트
                every { mockCouponService.getAvailableCoupons() } returns emptyList()
                sut.getAvailableCoupons()
                verify(exactly = 1) { mockCouponService.getAvailableCoupons() }
                verify(exactly = 0) { mockCouponService.getUserCoupons(any()) }
                verify(exactly = 0) { mockCouponService.getAvailableUserCoupons(any()) }

                clearMocks(mockCouponService)

                // getUserCoupons 테스트
                every { mockCouponService.getUserCoupons(userId) } returns emptyList()
                sut.getUserCoupons(userId)
                verify(exactly = 1) { mockCouponService.getUserCoupons(userId) }
                verify(exactly = 0) { mockCouponService.getAvailableCoupons() }
                verify(exactly = 0) { mockCouponService.getAvailableUserCoupons(any()) }

                clearMocks(mockCouponService)

                // getAvailableUserCoupons 테스트
                every { mockCouponService.getAvailableUserCoupons(userId) } returns emptyList()
                sut.getAvailableUserCoupons(userId)
                verify(exactly = 1) { mockCouponService.getAvailableUserCoupons(userId) }
                verify(exactly = 0) { mockCouponService.getAvailableCoupons() }
                verify(exactly = 0) { mockCouponService.getUserCoupons(any()) }
            }
        }
    }
})