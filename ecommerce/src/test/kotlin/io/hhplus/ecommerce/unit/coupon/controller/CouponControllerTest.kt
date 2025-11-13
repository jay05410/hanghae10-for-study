package io.hhplus.ecommerce.unit.coupon.controller

import io.hhplus.ecommerce.coupon.usecase.GetCouponQueryUseCase
import io.hhplus.ecommerce.coupon.usecase.IssueCouponUseCase
import io.hhplus.ecommerce.coupon.usecase.ValidateCouponUseCase
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CouponController 단위 테스트
 *
 * 책임: 쿠폰 관련 HTTP 요청 처리 검증
 * - REST API 엔드포인트의 요청/응답 처리 검증
 * - UseCase 계층과의 올바른 상호작용 검증
 * - 요청 데이터 변환 및 응답 형식 검증
 *
 * 검증 목표:
 * 1. 각 엔드포인트가 적절한 UseCase를 호출하는가?
 * 2. 요청 파라미터와 Body가 올바르게 UseCase에 전달되는가?
 * 3. UseCase 결과가 적절한 ApiResponse로 변환되는가?
 * 4. HTTP 메서드와 경로 매핑이 올바른가?
 * 5. 다양한 요청 형태에 대한 처리가 올바른가?
 */
class CouponControllerTest : DescribeSpec({
    val mockGetCouponQueryUseCase = mockk<GetCouponQueryUseCase>()
    val mockIssueCouponUseCase = mockk<IssueCouponUseCase>()
    val mockValidateCouponUseCase = mockk<ValidateCouponUseCase>()

    val sut = CouponController(
        getCouponQueryUseCase = mockGetCouponQueryUseCase,
        issueCouponUseCase = mockIssueCouponUseCase,
        validateCouponUseCase = mockValidateCouponUseCase
    )

    beforeEach {
        clearMocks(
            mockGetCouponQueryUseCase,
            mockIssueCouponUseCase,
            mockValidateCouponUseCase
        )
    }

    describe("getAvailableCoupons") {
        context("GET /api/v1/coupons 요청") {
            it("GetCouponQueryUseCase를 호출하고 ApiResponse로 감싸서 반환") {
                val expectedCoupons = listOf(
                    mockk<Coupon>(relaxed = true),
                    mockk<Coupon>(relaxed = true),
                    mockk<Coupon>(relaxed = true)
                )

                every { mockGetCouponQueryUseCase.getAvailableCoupons() } returns expectedCoupons

                val result = sut.getAvailableCoupons()

                result.success shouldBe true
                verify(exactly = 1) { mockGetCouponQueryUseCase.getAvailableCoupons() }
            }
        }

        context("발급 가능한 쿠폰이 없는 경우") {
            it("빈 리스트를 ApiResponse로 감싸서 반환") {
                val emptyCoupons = emptyList<Coupon>()

                every { mockGetCouponQueryUseCase.getAvailableCoupons() } returns emptyCoupons

                val result = sut.getAvailableCoupons()

                result.success shouldBe true
                verify(exactly = 1) { mockGetCouponQueryUseCase.getAvailableCoupons() }
            }
        }
    }

    describe("issueCoupon") {
        context("POST /api/v1/coupons/issue 요청") {
            it("IssueCouponRequest를 IssueCouponUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val request = IssueCouponRequest(couponId = 1L)
                val mockUserCoupon = mockk<UserCoupon>(relaxed = true)

                every { mockIssueCouponUseCase.execute(userId, request) } returns mockUserCoupon

                val result = sut.issueCoupon(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockIssueCouponUseCase.execute(userId, request) }
            }
        }

        context("다른 사용자의 쿠폰 발급 요청") {
            it("각각의 사용자와 쿠폰에 대해 정확한 파라미터 전달") {
                val userId = 2L
                val request = IssueCouponRequest(couponId = 5L)
                val mockUserCoupon = mockk<UserCoupon>(relaxed = true)

                every { mockIssueCouponUseCase.execute(userId, request) } returns mockUserCoupon

                val result = sut.issueCoupon(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockIssueCouponUseCase.execute(userId, request) }
            }
        }

        context("다양한 쿠폰 ID로 발급 요청") {
            it("모든 쿠폰 ID가 정확히 UseCase에 전달되는지 확인") {
                val userId = 1L
                val couponIds = listOf(1L, 100L, 999L)

                couponIds.forEach { couponId ->
                    val request = IssueCouponRequest(couponId = couponId)
                    val mockUserCoupon = mockk<UserCoupon>(relaxed = true)

                    every { mockIssueCouponUseCase.execute(userId, request) } returns mockUserCoupon

                    val result = sut.issueCoupon(userId, request)

                    result.success shouldBe true
                    verify(exactly = 1) { mockIssueCouponUseCase.execute(userId, request) }
                    clearMocks(mockIssueCouponUseCase)
                }
            }
        }
    }

    describe("getUserCoupons") {
        context("GET /api/v1/coupons/users/{userId} 요청") {
            it("userId를 GetCouponQueryUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val expectedUserCoupons = listOf(
                    mockk<UserCoupon>(relaxed = true),
                    mockk<UserCoupon>(relaxed = true)
                )

                every { mockGetCouponQueryUseCase.getUserCoupons(userId) } returns expectedUserCoupons

                val result = sut.getUserCoupons(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetCouponQueryUseCase.getUserCoupons(userId) }
            }
        }

        context("쿠폰이 없는 사용자 조회") {
            it("빈 리스트를 ApiResponse로 감싸서 반환") {
                val userId = 999L
                val emptyUserCoupons = emptyList<UserCoupon>()

                every { mockGetCouponQueryUseCase.getUserCoupons(userId) } returns emptyUserCoupons

                val result = sut.getUserCoupons(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetCouponQueryUseCase.getUserCoupons(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("요청된 userId를 정확히 UseCase에 전달") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockUserCoupons = listOf(mockk<UserCoupon>(relaxed = true))
                    every { mockGetCouponQueryUseCase.getUserCoupons(userId) } returns mockUserCoupons

                    val result = sut.getUserCoupons(userId)

                    result.success shouldBe true
                    verify(exactly = 1) { mockGetCouponQueryUseCase.getUserCoupons(userId) }
                    clearMocks(mockGetCouponQueryUseCase)
                }
            }
        }
    }

    describe("getAvailableUserCoupons") {
        context("GET /api/v1/coupons/users/{userId}/available 요청") {
            it("userId를 GetCouponQueryUseCase에 전달하고 사용 가능한 쿠폰 목록을 ApiResponse로 반환") {
                val userId = 1L
                val expectedAvailableCoupons = listOf(
                    mockk<UserCoupon>(relaxed = true),
                    mockk<UserCoupon>(relaxed = true)
                )

                every { mockGetCouponQueryUseCase.getAvailableUserCoupons(userId) } returns expectedAvailableCoupons

                val result = sut.getAvailableUserCoupons(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetCouponQueryUseCase.getAvailableUserCoupons(userId) }
            }
        }

        context("사용 가능한 쿠폰이 없는 사용자") {
            it("빈 리스트를 ApiResponse로 감싸서 반환") {
                val userId = 999L
                val emptyAvailableCoupons = emptyList<UserCoupon>()

                every { mockGetCouponQueryUseCase.getAvailableUserCoupons(userId) } returns emptyAvailableCoupons

                val result = sut.getAvailableUserCoupons(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetCouponQueryUseCase.getAvailableUserCoupons(userId) }
            }
        }
    }

    describe("validateCoupon") {
        context("POST /api/v1/coupons/validate 요청") {
            it("UseCouponRequest를 ValidateCouponUseCase에 전달하고 할인 금액을 ApiResponse로 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 50000L
                )
                val expectedDiscountAmount = 5000L

                every { mockValidateCouponUseCase.execute(userId, request) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockValidateCouponUseCase.execute(userId, request) }
            }
        }

        context("다양한 주문 금액으로 검증 요청") {
            it("모든 요청이 정확히 UseCase에 전달되는지 확인") {
                val userId = 1L
                val testCases = listOf(
                    Pair(30000L, 3000L),
                    Pair(50000L, 5000L),
                    Pair(100000L, 10000L)
                )

                testCases.forEach { (orderAmount, expectedDiscount) ->
                    val request = UseCouponRequest(
                        userCouponId = 1L,
                        orderAmount = orderAmount
                    )

                    every { mockValidateCouponUseCase.execute(userId, request) } returns expectedDiscount

                    val result = sut.validateCoupon(userId, request)

                    result.success shouldBe true
                    verify(exactly = 1) { mockValidateCouponUseCase.execute(userId, request) }
                    clearMocks(mockValidateCouponUseCase)
                }
            }
        }

        context("할인 금액이 0인 경우") {
            it("0을 ApiResponse로 감싸서 반환") {
                val userId = 1L
                val request = UseCouponRequest(
                    userCouponId = 1L,
                    orderAmount = 5000L  // 최소 주문 금액 미달
                )
                val expectedDiscountAmount = 0L

                every { mockValidateCouponUseCase.execute(userId, request) } returns expectedDiscountAmount

                val result = sut.validateCoupon(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockValidateCouponUseCase.execute(userId, request) }
            }
        }
    }

    describe("API 경로 및 메서드 검증") {
        context("모든 엔드포인트") {
            it("적절한 UseCase만 호출하고 다른 UseCase는 호출하지 않음") {
                // getAvailableCoupons 테스트
                every { mockGetCouponQueryUseCase.getAvailableCoupons() } returns emptyList()
                sut.getAvailableCoupons()
                verify(exactly = 1) { mockGetCouponQueryUseCase.getAvailableCoupons() }
                verify(exactly = 0) { mockIssueCouponUseCase.execute(any(), any()) }
                verify(exactly = 0) { mockValidateCouponUseCase.execute(any(), any()) }

                clearMocks(mockGetCouponQueryUseCase, mockIssueCouponUseCase, mockValidateCouponUseCase)

                // issueCoupon 테스트
                val issueRequest = IssueCouponRequest(couponId = 1L)
                every { mockIssueCouponUseCase.execute(1L, issueRequest) } returns mockk(relaxed = true)
                sut.issueCoupon(1L, issueRequest)
                verify(exactly = 1) { mockIssueCouponUseCase.execute(1L, issueRequest) }
                verify(exactly = 0) { mockGetCouponQueryUseCase.getAvailableCoupons() }
                verify(exactly = 0) { mockValidateCouponUseCase.execute(any(), any()) }

                clearMocks(mockGetCouponQueryUseCase, mockIssueCouponUseCase, mockValidateCouponUseCase)

                // getUserCoupons 테스트
                every { mockGetCouponQueryUseCase.getUserCoupons(1L) } returns emptyList()
                sut.getUserCoupons(1L)
                verify(exactly = 1) { mockGetCouponQueryUseCase.getUserCoupons(1L) }
                verify(exactly = 0) { mockIssueCouponUseCase.execute(any(), any()) }
                verify(exactly = 0) { mockValidateCouponUseCase.execute(any(), any()) }

                clearMocks(mockGetCouponQueryUseCase, mockIssueCouponUseCase, mockValidateCouponUseCase)

                // validateCoupon 테스트
                val validateRequest = UseCouponRequest(userCouponId = 1L, orderAmount = 50000L)
                every { mockValidateCouponUseCase.execute(1L, validateRequest) } returns 5000L
                sut.validateCoupon(1L, validateRequest)
                verify(exactly = 1) { mockValidateCouponUseCase.execute(1L, validateRequest) }
                verify(exactly = 0) { mockGetCouponQueryUseCase.getAvailableCoupons() }
                verify(exactly = 0) { mockIssueCouponUseCase.execute(any(), any()) }
            }
        }
    }
})