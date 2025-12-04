package io.hhplus.ecommerce.unit.coupon.usecase

import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.coupon.domain.vo.CouponCacheInfo
import io.hhplus.ecommerce.coupon.application.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.presentation.dto.UseCouponRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * CouponCommandUseCase 단위 테스트 (검증 기능)
 *
 * 책임: 쿠폰 사용 검증 비즈니스 흐름 검증
 * - UserCoupon 조회 및 사용 가능 여부 검증
 * - CouponCacheInfo 기반 할인 금액 계산
 *
 * 검증 목표:
 * 1. UserCoupon이 사용 가능한 상태인지 확인
 * 2. CouponCacheInfo로 최소 주문 금액 및 할인 계산
 * 3. 다양한 요청 데이터에 대한 처리가 올바른가?
 */
class CouponCommandUseCaseValidateTest : DescribeSpec({
    val mockCouponDomainService = mockk<CouponDomainService>()
    val sut = CouponCommandUseCase(mockCouponDomainService, mockk(relaxed = true), mockk(relaxed = true))

    beforeEach {
        clearMocks(mockCouponDomainService)
    }

    fun createCouponCacheInfo(
        couponId: Long = 100L,
        discountType: DiscountType = DiscountType.FIXED,
        discountValue: Long = 5000L,
        minimumOrderAmount: Long = 10000L
    ) = CouponCacheInfo(
        id = couponId,
        name = "테스트쿠폰",
        code = "TEST",
        discountType = discountType,
        discountValue = discountValue,
        minimumOrderAmount = minimumOrderAmount,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(30)
    )

    describe("validateCoupon") {
        context("정상적인 쿠폰 검증 요청") {
            it("CouponCacheInfo로 할인 금액을 계산하여 반환") {
                val userId = 1L
                val userCouponId = 1L
                val couponId = 100L
                val orderAmount = 50000L
                val expectedDiscountAmount = 5000L

                val mockUserCoupon = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId
                    every { isUsable() } returns true
                }
                val couponCacheInfo = createCouponCacheInfo(couponId, DiscountType.FIXED, expectedDiscountAmount, 10000L)
                val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                every { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId) } returns couponCacheInfo

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) }
                verify(exactly = 1) { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId) }
            }
        }

        context("다양한 주문 금액으로 검증 요청") {
            it("모든 주문 금액이 정확히 처리되는지 확인") {
                val userId = 1L
                val userCouponId = 1L
                val couponId = 100L
                val orderAmounts = listOf(10000L, 50000L, 100000L)
                val discountAmounts = listOf(1000L, 5000L, 10000L)

                orderAmounts.forEachIndexed { index, orderAmount ->
                    val mockUserCoupon = mockk<UserCoupon> {
                        every { this@mockk.couponId } returns couponId
                        every { isUsable() } returns true
                    }
                    val couponCacheInfo = createCouponCacheInfo(couponId, DiscountType.FIXED, discountAmounts[index], 5000L)
                    val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                    every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                    every { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId) } returns couponCacheInfo

                    val result = sut.validateCoupon(userId, request)

                    result shouldBe discountAmounts[index]
                    clearMocks(mockCouponDomainService)
                }
            }
        }

        context("할인 금액이 0인 경우") {
            it("0을 반환") {
                val userId = 1L
                val userCouponId = 1L
                val couponId = 100L
                val orderAmount = 5000L
                val expectedDiscountAmount = 0L

                val mockUserCoupon = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId
                    every { isUsable() } returns true
                }
                val couponCacheInfo = createCouponCacheInfo(couponId, DiscountType.FIXED, expectedDiscountAmount, 1000L)
                val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                every { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId) } returns couponCacheInfo

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
            }
        }

        context("큰 주문 금액의 경우") {
            it("큰 할인 금액도 정확히 반환") {
                val userId = 1L
                val userCouponId = 1L
                val couponId = 100L
                val orderAmount = 1000000L
                val expectedDiscountAmount = 100000L

                val mockUserCoupon = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId
                    every { isUsable() } returns true
                }
                val couponCacheInfo = createCouponCacheInfo(couponId, DiscountType.FIXED, expectedDiscountAmount, 10000L)
                val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                every { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId) } returns couponCacheInfo

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
            }
        }

        context("연속된 검증 요청") {
            it("매번 서비스를 호출하고 결과를 반환") {
                val userId = 1L
                val userCouponId1 = 1L
                val userCouponId2 = 2L
                val couponId1 = 100L
                val couponId2 = 200L
                val orderAmount1 = 30000L
                val orderAmount2 = 60000L
                val expectedDiscountAmount1 = 3000L
                val expectedDiscountAmount2 = 6000L

                val mockUserCoupon1 = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId1
                    every { isUsable() } returns true
                }
                val mockUserCoupon2 = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId2
                    every { isUsable() } returns true
                }
                val couponCacheInfo1 = createCouponCacheInfo(couponId1, DiscountType.FIXED, expectedDiscountAmount1, 10000L)
                val couponCacheInfo2 = createCouponCacheInfo(couponId2, DiscountType.FIXED, expectedDiscountAmount2, 10000L)
                val request1 = UseCouponRequest(userCouponId = userCouponId1, orderAmount = orderAmount1)
                val request2 = UseCouponRequest(userCouponId = userCouponId2, orderAmount = orderAmount2)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId1, userId) } returns mockUserCoupon1
                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId2, userId) } returns mockUserCoupon2
                every { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId1) } returns couponCacheInfo1
                every { mockCouponDomainService.getCouponCacheInfoOrThrow(couponId2) } returns couponCacheInfo2

                val result1 = sut.validateCoupon(userId, request1)
                val result2 = sut.validateCoupon(userId, request2)

                result1 shouldBe expectedDiscountAmount1
                result2 shouldBe expectedDiscountAmount2
            }
        }
    }
})