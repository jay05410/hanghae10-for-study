package io.hhplus.ecommerce.unit.coupon.application

import io.hhplus.ecommerce.coupon.application.CouponIssueHistoryService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CouponService 단위 테스트
 *
 * 책임: 쿠폰 도메인 서비스의 핵심 비즈니스 로직 검증
 * - 쿠폰 조회, 발급, 사용, 검증 기능의 Repository 호출 검증
 * - CouponIssueHistoryService와의 상호작용 검증
 * - 도메인 객체와의 상호작용 검증
 *
 * 검증 목표:
 * 1. 각 메서드가 적절한 Repository 및 의존 서비스 메서드를 호출하는가?
 * 2. 도메인 객체의 비즈니스 메서드가 올바르게 호출되는가?
 * 3. 예외 상황에서 적절한 예외가 발생하는가?
 * 4. 쿠폰 발급/사용 이력 기록이 올바르게 수행되는가?
 */
class CouponServiceTest : DescribeSpec({
    val mockCouponRepository = mockk<CouponRepository>()
    val mockUserCouponRepository = mockk<UserCouponRepository>()
    val mockCouponIssueHistoryService = mockk<CouponIssueHistoryService>()
    val sut = CouponService(mockCouponRepository, mockUserCouponRepository, mockCouponIssueHistoryService)

    beforeEach {
        clearMocks(mockCouponRepository, mockUserCouponRepository, mockCouponIssueHistoryService)
    }

    describe("getAvailableCoupons") {
        context("발급 가능한 쿠폰들이 있는 경우") {
            it("Repository에서 조회한 쿠폰 중 발급 가능한 것들만 필터링하여 반환") {
                val availableCoupon = mockk<Coupon> {
                    every { isAvailableForIssue() } returns true
                }
                val unavailableCoupon = mockk<Coupon> {
                    every { isAvailableForIssue() } returns false
                }
                val coupons = listOf(availableCoupon, unavailableCoupon)

                every { mockCouponRepository.findAvailableCoupons() } returns coupons

                val result = sut.getAvailableCoupons()

                result shouldBe listOf(availableCoupon)
                verify(exactly = 1) { mockCouponRepository.findAvailableCoupons() }
                verify(exactly = 1) { availableCoupon.isAvailableForIssue() }
                verify(exactly = 1) { unavailableCoupon.isAvailableForIssue() }
            }
        }

        context("발급 가능한 쿠폰이 없는 경우") {
            it("빈 리스트를 반환") {
                every { mockCouponRepository.findAvailableCoupons() } returns emptyList()

                val result = sut.getAvailableCoupons()

                result shouldBe emptyList()
                verify(exactly = 1) { mockCouponRepository.findAvailableCoupons() }
            }
        }
    }

    describe("getCouponByName") {
        context("존재하는 쿠폰명으로 조회") {
            it("Repository에 조회를 지시하고 쿠폰을 반환") {
                val couponName = "할인쿠폰"
                val expectedCoupon = mockk<Coupon>()

                every { mockCouponRepository.findByName(couponName) } returns expectedCoupon

                val result = sut.getCouponByName(couponName)

                result shouldBe expectedCoupon
                verify(exactly = 1) { mockCouponRepository.findByName(couponName) }
            }
        }

        context("존재하지 않는 쿠폰명으로 조회") {
            it("null을 반환") {
                val couponName = "없는쿠폰"

                every { mockCouponRepository.findByName(couponName) } returns null

                val result = sut.getCouponByName(couponName)

                result shouldBe null
                verify(exactly = 1) { mockCouponRepository.findByName(couponName) }
            }
        }
    }

    describe("issueCoupon") {
        context("정상적인 쿠폰 발급") {
            it("쿠폰을 검증하고 발급한 후 이력을 기록") {
                val userId = 1L
                val couponId = 1L
                val mockCoupon = mockk<Coupon>(relaxed = true) {
                    every { id } returns couponId
                    every { name } returns "할인쿠폰"
                    every { isAvailableForIssue() } returns true
                }
                val mockUserCoupon = mockk<UserCoupon>()

                every { mockCoupon.issue() } just runs
                every { mockCouponRepository.findByIdWithLock(couponId) } returns mockCoupon
                every { mockUserCouponRepository.findByUserIdAndCouponId(userId, couponId) } returns null
                every { mockCouponRepository.save(mockCoupon) } returns mockCoupon
                every { mockUserCouponRepository.save(any()) } returns mockUserCoupon
                every { mockCouponIssueHistoryService.recordIssue(couponId, userId, "할인쿠폰") } returns mockk()

                mockkObject(UserCoupon.Companion)
                every { UserCoupon.create(userId, couponId) } returns mockUserCoupon

                val result = sut.issueCoupon(userId, couponId)

                result shouldBe mockUserCoupon
                verifyOrder {
                    mockCouponRepository.findByIdWithLock(couponId)
                    mockCoupon.isAvailableForIssue()
                    mockUserCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    mockCoupon.issue()
                    mockCouponRepository.save(mockCoupon)
                    UserCoupon.create(userId, couponId)
                    mockUserCouponRepository.save(mockUserCoupon)
                    mockCouponIssueHistoryService.recordIssue(couponId, userId, "할인쿠폰")
                }
            }
        }

        context("존재하지 않는 쿠폰 발급 시도") {
            it("CouponException.CouponNotFound를 발생") {
                val userId = 1L
                val couponId = 999L

                every { mockCouponRepository.findByIdWithLock(couponId) } returns null

                shouldThrow<CouponException.CouponNotFound> {
                    sut.issueCoupon(userId, couponId)
                }

                verify(exactly = 1) { mockCouponRepository.findByIdWithLock(couponId) }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
                verify(exactly = 0) { mockCouponIssueHistoryService.recordIssue(any(), any(), any()) }
            }
        }

        context("발급 불가능한 쿠폰 발급 시도") {
            it("CouponException.CouponSoldOut을 발생") {
                val userId = 1L
                val couponId = 1L
                val mockCoupon = mockk<Coupon> {
                    every { name } returns "품절쿠폰"
                    every { getRemainingQuantity() } returns 0
                    every { isAvailableForIssue() } returns false
                }

                every { mockCouponRepository.findByIdWithLock(couponId) } returns mockCoupon

                shouldThrow<CouponException.CouponSoldOut> {
                    sut.issueCoupon(userId, couponId)
                }

                verify(exactly = 1) { mockCouponRepository.findByIdWithLock(couponId) }
                verify(exactly = 1) { mockCoupon.isAvailableForIssue() }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
            }
        }

        context("이미 발급받은 쿠폰 재발급 시도") {
            it("CouponException.AlreadyIssuedCoupon을 발생") {
                val userId = 1L
                val couponId = 1L
                val mockCoupon = mockk<Coupon> {
                    every { name } returns "이미발급쿠폰"
                    every { isAvailableForIssue() } returns true
                }
                val existingUserCoupon = mockk<UserCoupon>()

                every { mockCouponRepository.findByIdWithLock(couponId) } returns mockCoupon
                every { mockUserCouponRepository.findByUserIdAndCouponId(userId, couponId) } returns existingUserCoupon

                shouldThrow<CouponException.AlreadyIssuedCoupon> {
                    sut.issueCoupon(userId, couponId)
                }

                verify(exactly = 1) { mockCouponRepository.findByIdWithLock(couponId) }
                verify(exactly = 1) { mockUserCouponRepository.findByUserIdAndCouponId(userId, couponId) }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
            }
        }
    }

    describe("getUserCoupons") {
        context("사용자의 모든 쿠폰 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val userId = 1L
                val expectedCoupons = listOf(mockk<UserCoupon>(), mockk<UserCoupon>())

                every { mockUserCouponRepository.findByUserId(userId) } returns expectedCoupons

                val result = sut.getUserCoupons(userId)

                result shouldBe expectedCoupons
                verify(exactly = 1) { mockUserCouponRepository.findByUserId(userId) }
            }
        }
    }

    describe("getAvailableUserCoupons") {
        context("사용 가능한 쿠폰들이 있는 경우") {
            it("발급된 쿠폰 중 사용 가능한 것들만 필터링하여 반환") {
                val userId = 1L
                val usableCoupon = UserCoupon(
                    id = 1L,
                    userId = userId,
                    couponId = 1L,
                    status = UserCouponStatus.ISSUED
                )
                val unusableCoupon = UserCoupon(
                    id = 2L,
                    userId = userId,
                    couponId = 2L,
                    status = UserCouponStatus.USED
                )
                val issuedCoupons = listOf(usableCoupon, unusableCoupon)

                every { mockUserCouponRepository.findByUserIdAndStatus(userId, UserCouponStatus.ISSUED) } returns issuedCoupons

                val result = sut.getAvailableUserCoupons(userId)

                result shouldBe listOf(usableCoupon)
                verify(exactly = 1) { mockUserCouponRepository.findByUserIdAndStatus(userId, UserCouponStatus.ISSUED) }
                verify(exactly = 1) { usableCoupon.isUsable() }
                verify(exactly = 1) { unusableCoupon.isUsable() }
            }
        }
    }

    describe("applyCoupon") {
        context("정상적인 쿠폰 사용") {
            it("쿠폰을 검증하고 사용한 후 이력을 기록") {
                val userId = 1L
                val userCouponId = 1L
                val orderId = 1L
                val orderAmount = 50000L
                val discountAmount = 5000L

                val mockUserCoupon = mockk<UserCoupon>(relaxed = true) {
                    every { id } returns userCouponId
                    every { couponId } returns 1L
                    every { isUsable() } returns true
                }
                val mockCoupon = mockk<Coupon> {
                    every { id } returns 1L
                    every { name } returns "할인쿠폰"
                    every { isValidForUse(orderAmount) } returns true
                    every { calculateDiscountAmount(orderAmount) } returns discountAmount
                }

                every { mockUserCoupon.use(orderId) } just runs
                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(1L) } returns mockCoupon
                every { mockUserCouponRepository.save(mockUserCoupon) } returns mockUserCoupon
                every { mockCouponIssueHistoryService.recordUsage(any(), any(), any(), any(), any()) } returns mockk()

                val result = sut.applyCoupon(userId, userCouponId, orderId, orderAmount)

                result shouldBe discountAmount
                verifyOrder {
                    mockUserCouponRepository.findById(userCouponId)
                    mockCouponRepository.findById(1L)
                    mockUserCoupon.isUsable()
                    mockCoupon.isValidForUse(orderAmount)
                    mockCoupon.calculateDiscountAmount(orderAmount)
                    mockUserCoupon.use(orderId)
                    mockUserCouponRepository.save(mockUserCoupon)
                    mockCouponIssueHistoryService.recordUsage(1L, userId, "할인쿠폰", orderId, any())
                }
            }
        }

        context("존재하지 않는 사용자 쿠폰으로 사용 시도") {
            it("CouponException.UserCouponNotFound를 발생") {
                val userId = 1L
                val userCouponId = 999L
                val orderId = 1L
                val orderAmount = 50000L

                every { mockUserCouponRepository.findById(userCouponId) } returns null

                shouldThrow<CouponException.UserCouponNotFound> {
                    sut.applyCoupon(userId, userCouponId, orderId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
            }
        }

        context("쿠폰이 존재하지 않는 경우") {
            it("CouponException.CouponNotFound를 발생") {
                val userId = 1L
                val userCouponId = 1L
                val orderId = 1L
                val orderAmount = 50000L
                val mockUserCoupon = mockk<UserCoupon> {
                    every { couponId } returns 999L
                }

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(999L) } returns null

                shouldThrow<CouponException.CouponNotFound> {
                    sut.applyCoupon(userId, userCouponId, orderId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(999L) }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
            }
        }

        context("사용 불가능한 쿠폰 사용 시도") {
            it("CouponException.AlreadyUsedCoupon을 발생") {
                val userId = 1L
                val userCouponId = 1L
                val orderId = 1L
                val orderAmount = 50000L
                val mockUserCoupon = mockk<UserCoupon> {
                    every { id } returns userCouponId
                    every { couponId } returns 1L
                    every { isUsable() } returns false
                }
                val mockCoupon = mockk<Coupon>()

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(1L) } returns mockCoupon

                shouldThrow<CouponException.AlreadyUsedCoupon> {
                    sut.applyCoupon(userId, userCouponId, orderId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(1L) }
                verify(exactly = 1) { mockUserCoupon.isUsable() }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
            }
        }

        context("최소 주문 금액 미달") {
            it("CouponException.MinimumOrderAmountNotMet을 발생") {
                val userId = 1L
                val userCouponId = 1L
                val orderId = 1L
                val orderAmount = 10000L
                val mockUserCoupon = mockk<UserCoupon> {
                    every { couponId } returns 1L
                    every { isUsable() } returns true
                }
                val mockCoupon = mockk<Coupon>(relaxed = true) {
                    every { name } returns "할인쿠폰"
                    every { isValidForUse(orderAmount) } returns false
                }

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(1L) } returns mockCoupon

                shouldThrow<CouponException.MinimumOrderAmountNotMet> {
                    sut.applyCoupon(userId, userCouponId, orderId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(1L) }
                verify(exactly = 1) { mockUserCoupon.isUsable() }
                verify(exactly = 1) { mockCoupon.isValidForUse(orderAmount) }
                verify(exactly = 0) { mockUserCouponRepository.save(any()) }
            }
        }
    }

    describe("validateCouponUsage") {
        context("유효한 쿠폰 사용 검증") {
            it("할인 금액을 계산하여 반환") {
                val userId = 1L
                val userCouponId = 1L
                val orderAmount = 50000L
                val discountAmount = 5000L

                val mockUserCoupon = mockk<UserCoupon> {
                    every { couponId } returns 1L
                    every { isUsable() } returns true
                }
                val mockCoupon = mockk<Coupon> {
                    every { isValidForUse(orderAmount) } returns true
                    every { calculateDiscountAmount(orderAmount) } returns discountAmount
                }

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(1L) } returns mockCoupon

                val result = sut.validateCouponUsage(userId, userCouponId, orderAmount)

                result shouldBe discountAmount
                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(1L) }
                verify(exactly = 1) { mockUserCoupon.isUsable() }
                verify(exactly = 1) { mockCoupon.isValidForUse(orderAmount) }
                verify(exactly = 1) { mockCoupon.calculateDiscountAmount(orderAmount) }
            }
        }

        context("존재하지 않는 사용자 쿠폰으로 검증 시도") {
            it("CouponException.UserCouponNotFound를 발생") {
                val userId = 1L
                val userCouponId = 999L
                val orderAmount = 50000L

                every { mockUserCouponRepository.findById(userCouponId) } returns null

                shouldThrow<CouponException.UserCouponNotFound> {
                    sut.validateCouponUsage(userId, userCouponId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 0) { mockCouponRepository.findById(any()) }
            }
        }

        context("쿠폰이 존재하지 않는 경우") {
            it("CouponException.CouponNotFound를 발생") {
                val userId = 1L
                val userCouponId = 1L
                val orderAmount = 50000L
                val mockUserCoupon = mockk<UserCoupon> {
                    every { couponId } returns 999L
                }

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(999L) } returns null

                shouldThrow<CouponException.CouponNotFound> {
                    sut.validateCouponUsage(userId, userCouponId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(999L) }
            }
        }

        context("사용 불가능한 쿠폰으로 검증 시도") {
            it("CouponException.AlreadyUsedCoupon을 발생") {
                val userId = 1L
                val userCouponId = 1L
                val orderAmount = 50000L
                val mockUserCoupon = mockk<UserCoupon> {
                    every { id } returns userCouponId
                    every { couponId } returns 1L
                    every { isUsable() } returns false
                }
                val mockCoupon = mockk<Coupon>()

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(1L) } returns mockCoupon

                shouldThrow<CouponException.AlreadyUsedCoupon> {
                    sut.validateCouponUsage(userId, userCouponId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(1L) }
                verify(exactly = 1) { mockUserCoupon.isUsable() }
            }
        }

        context("최소 주문 금액 미달로 검증 실패") {
            it("CouponException.MinimumOrderAmountNotMet을 발생") {
                val userId = 1L
                val userCouponId = 1L
                val orderAmount = 10000L
                val mockUserCoupon = mockk<UserCoupon> {
                    every { couponId } returns 1L
                    every { isUsable() } returns true
                }
                val mockCoupon = mockk<Coupon>(relaxed = true) {
                    every { name } returns "할인쿠폰"
                    every { isValidForUse(orderAmount) } returns false
                }

                every { mockUserCouponRepository.findById(userCouponId) } returns mockUserCoupon
                every { mockCouponRepository.findById(1L) } returns mockCoupon

                shouldThrow<CouponException.MinimumOrderAmountNotMet> {
                    sut.validateCouponUsage(userId, userCouponId, orderAmount)
                }

                verify(exactly = 1) { mockUserCouponRepository.findById(userCouponId) }
                verify(exactly = 1) { mockCouponRepository.findById(1L) }
                verify(exactly = 1) { mockUserCoupon.isUsable() }
                verify(exactly = 1) { mockCoupon.isValidForUse(orderAmount) }
            }
        }
    }

    describe("getCouponIssueHistory") {
        context("사용자의 쿠폰 발급 이력 조회") {
            it("CouponIssueHistoryService에 조회를 위임하고 결과를 반환") {
                val userId = 1L
                val expectedHistory = listOf(mockk<CouponIssueHistory>())

                every { mockCouponIssueHistoryService.getUserCouponHistory(userId) } returns expectedHistory

                val result = sut.getCouponIssueHistory(userId)

                result shouldBe expectedHistory
                verify(exactly = 1) { mockCouponIssueHistoryService.getUserCouponHistory(userId) }
            }
        }
    }

    describe("getCouponIssueStatistics") {
        context("쿠폰 발급 통계 조회") {
            it("CouponIssueHistoryService에 조회를 위임하고 결과를 반환") {
                val couponId = 1L
                val expectedStats = CouponIssueHistoryService.CouponStatistics(
                    couponId = couponId,
                    totalIssued = 100,
                    totalUsed = 80,
                    totalExpired = 5,
                    usageRate = 80.0
                )

                every { mockCouponIssueHistoryService.getCouponStatistics(couponId) } returns expectedStats

                val result = sut.getCouponIssueStatistics(couponId)

                result shouldBe expectedStats
                verify(exactly = 1) { mockCouponIssueHistoryService.getCouponStatistics(couponId) }
            }
        }
    }
})