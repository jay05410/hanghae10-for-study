package io.hhplus.ecommerce.unit.coupon.usecase

import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.coupon.application.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.presentation.dto.UseCouponRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CouponCommandUseCase 단위 테스트 (검증 기능)
 *
 * 책임: 쿠폰 사용 검증 비즈니스 흐름 검증
 * - 쿠폰 사용 가능성 검증 로직의 서비스 위임 검증
 * - 할인 금액 계산 결과 반환 검증
 *
 * 검증 목표:
 * 1. CouponDomainService에 올바른 파라미터가 전달되는가?
 * 2. 서비스 결과가 그대로 반환되는가?
 * 3. 다양한 요청 데이터에 대한 처리가 올바른가?
 */
class CouponCommandUseCaseValidateTest : DescribeSpec({
    val mockCouponDomainService = mockk<CouponDomainService>()
    val sut = CouponCommandUseCase(mockCouponDomainService, mockk(relaxed = true), mockk(relaxed = true))

    beforeEach {
        clearMocks(mockCouponDomainService)
    }

    describe("validateCoupon") {
        context("정상적인 쿠폰 검증 요청") {
            it("CouponDomainService에 검증을 위임하고 할인 금액을 반환") {
                val userId = 1L
                val userCouponId = 1L
                val couponId = 100L
                val orderAmount = 50000L
                val expectedDiscountAmount = 5000L

                val mockUserCoupon = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId
                }
                val mockCoupon = mockk<Coupon>()
                val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                every { mockCouponDomainService.getCouponOrThrow(couponId) } returns mockCoupon
                every { mockCouponDomainService.validateCouponUsage(mockUserCoupon, mockCoupon, orderAmount) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result shouldBe expectedDiscountAmount
                verify(exactly = 1) { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) }
                verify(exactly = 1) { mockCouponDomainService.getCouponOrThrow(couponId) }
                verify(exactly = 1) { mockCouponDomainService.validateCouponUsage(mockUserCoupon, mockCoupon, orderAmount) }
            }
        }

        context("다양한 주문 금액으로 검증 요청") {
            it("모든 주문 금액이 정확히 서비스에 전달되는지 확인") {
                val userId = 1L
                val userCouponId = 1L
                val couponId = 100L
                val orderAmounts = listOf(10000L, 50000L, 100000L)
                val discountAmounts = listOf(1000L, 5000L, 10000L)

                orderAmounts.forEachIndexed { index, orderAmount ->
                    val mockUserCoupon = mockk<UserCoupon> {
                        every { this@mockk.couponId } returns couponId
                    }
                    val mockCoupon = mockk<Coupon>()
                    val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                    every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                    every { mockCouponDomainService.getCouponOrThrow(couponId) } returns mockCoupon
                    every { mockCouponDomainService.validateCouponUsage(mockUserCoupon, mockCoupon, orderAmount) } returns discountAmounts[index]

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
                }
                val mockCoupon = mockk<Coupon>()
                val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                every { mockCouponDomainService.getCouponOrThrow(couponId) } returns mockCoupon
                every { mockCouponDomainService.validateCouponUsage(mockUserCoupon, mockCoupon, orderAmount) } returns expectedDiscountAmount

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
                }
                val mockCoupon = mockk<Coupon>()
                val request = UseCouponRequest(userCouponId = userCouponId, orderAmount = orderAmount)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId, userId) } returns mockUserCoupon
                every { mockCouponDomainService.getCouponOrThrow(couponId) } returns mockCoupon
                every { mockCouponDomainService.validateCouponUsage(mockUserCoupon, mockCoupon, orderAmount) } returns expectedDiscountAmount

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
                }
                val mockUserCoupon2 = mockk<UserCoupon> {
                    every { this@mockk.couponId } returns couponId2
                }
                val mockCoupon1 = mockk<Coupon>()
                val mockCoupon2 = mockk<Coupon>()
                val request1 = UseCouponRequest(userCouponId = userCouponId1, orderAmount = orderAmount1)
                val request2 = UseCouponRequest(userCouponId = userCouponId2, orderAmount = orderAmount2)

                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId1, userId) } returns mockUserCoupon1
                every { mockCouponDomainService.getUserCouponOrThrow(userCouponId2, userId) } returns mockUserCoupon2
                every { mockCouponDomainService.getCouponOrThrow(couponId1) } returns mockCoupon1
                every { mockCouponDomainService.getCouponOrThrow(couponId2) } returns mockCoupon2
                every { mockCouponDomainService.validateCouponUsage(mockUserCoupon1, mockCoupon1, orderAmount1) } returns expectedDiscountAmount1
                every { mockCouponDomainService.validateCouponUsage(mockUserCoupon2, mockCoupon2, orderAmount2) } returns expectedDiscountAmount2

                val result1 = sut.validateCoupon(userId, request1)
                val result2 = sut.validateCoupon(userId, request2)

                result1 shouldBe expectedDiscountAmount1
                result2 shouldBe expectedDiscountAmount2
            }
        }
    }
})