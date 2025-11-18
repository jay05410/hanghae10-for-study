package io.hhplus.ecommerce.integration.coupon

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.coupon.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.usecase.GetCouponQueryUseCase
import io.hhplus.ecommerce.coupon.usecase.ValidateCouponUseCase
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

/**
 * 쿠폰 발급 통합 테스트 (비동시성)
 *
 * TestContainers MySQL을 사용하여 쿠폰 발급 전체 플로우를 검증합니다.
 * - 쿠폰 발급
 * - 발급 가능 여부 검증
 * - 중복 발급 방지
 * - 쿠폰 사용 및 할인 적용
 * - 쿠폰 발급 이력 기록
 */
class CouponIssueIntegrationTest(
    private val couponCommandUseCase: CouponCommandUseCase,
    private val getCouponQueryUseCase: GetCouponQueryUseCase,
    private val validateCouponUseCase: ValidateCouponUseCase,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) : KotestIntegrationTestBase({

    describe("쿠폰 발급") {
        context("정상적인 쿠폰 발급 요청일 때") {
            it("쿠폰을 정상적으로 발급할 수 있다") {
                // Given
                val coupon = Coupon.create(
                    name = "신규 회원 가입 쿠폰",
                    code = "WELCOME2024",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 10,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 100,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                    createdBy = 1L
                )
                val savedCoupon = couponRepository.save(coupon)
                val userId = 1000L

                // When
                val userCoupon = couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(savedCoupon.id))

                // Then
                userCoupon shouldNotBe null
                userCoupon.userId shouldBe userId
                userCoupon.couponId shouldBe savedCoupon.id
                userCoupon.status shouldBe UserCouponStatus.ISSUED

                // 쿠폰 발급 수량 증가 확인
                val updatedCoupon = couponRepository.findById(savedCoupon.id)
                updatedCoupon!!.issuedQuantity shouldBe 1
            }
        }

        context("동일 쿠폰을 중복 발급하려 할 때") {
            it("예외가 발생한다") {
                // Given
                val coupon = Coupon.create(
                    name = "중복 발급 테스트 쿠폰",
                    code = "DUPLICATE_TEST",
                    discountType = DiscountType.FIXED,
                    discountValue = 5000,
                    minimumOrderAmount = 20000L,
                    totalQuantity = 50,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                    createdBy = 1L
                )
                val savedCoupon = couponRepository.save(coupon)
                val userId = 2000L

                // 첫 번째 발급
                couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(savedCoupon.id))

                // When & Then - 중복 발급 시도
                shouldThrow<CouponException.AlreadyIssuedCoupon> {
                    couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(savedCoupon.id))
                }
            }
        }

        context("쿠폰이 품절되었을 때") {
            it("예외가 발생한다") {
                // Given - 수량 1개 쿠폰
                val coupon = Coupon.create(
                    name = "한정 수량 쿠폰",
                    code = "LIMITED1",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 20,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 1,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                    createdBy = 1L
                )
                val savedCoupon = couponRepository.save(coupon)

                // 첫 번째 사용자가 발급받음
                couponCommandUseCase.issueCoupon(3000L, IssueCouponRequest(savedCoupon.id))

                // When & Then - 두 번째 사용자 발급 시도
                shouldThrow<CouponException.CouponSoldOut> {
                    couponCommandUseCase.issueCoupon(3001L, IssueCouponRequest(savedCoupon.id))
                }
            }
        }

        context("존재하지 않는 쿠폰 ID로 발급 시도할 때") {
            it("예외가 발생한다") {
                // When & Then
                shouldThrow<CouponException.CouponNotFound> {
                    couponCommandUseCase.issueCoupon(4000L, IssueCouponRequest(99999L))
                }
            }
        }
    }

    describe("쿠폰 조회") {
        context("발급 가능한 쿠폰 목록 조회 시") {
            it("활성화되고 수량이 남은 쿠폰만 조회된다") {
                // Given
                val now = LocalDateTime.now()

                // 정상 쿠폰
                val normalCoupon = Coupon.create(
                    name = "정상 쿠폰",
                    code = "NORMAL",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 10,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 10,
                    validFrom = now,
                    validTo = now.plusDays(30),
                    createdBy = 1L
                )
                couponRepository.save(normalCoupon)

                // 품절 쿠폰
                val soldOutCoupon = Coupon.create(
                    name = "품절 쿠폰",
                    code = "SOLDOUT",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 10,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 1,
                    validFrom = now,
                    validTo = now.plusDays(30),
                    createdBy = 1L
                )
                val saved = couponRepository.save(soldOutCoupon)
                couponCommandUseCase.issueCoupon(5000L, IssueCouponRequest(saved.id)) // 품절 처리

                // When
                val availableCoupons = getCouponQueryUseCase.getAvailableCoupons()

                // Then
                availableCoupons shouldHaveSize 1
                availableCoupons[0].name shouldBe "정상 쿠폰"
            }
        }

        context("사용자의 쿠폰 목록 조회 시") {
            it("발급받은 모든 쿠폰을 조회할 수 있다") {
                // Given
                val userId = 6000L
                val coupon1 = couponRepository.save(
                    Coupon.create(
                        name = "쿠폰1",
                        code = "COUPON1",
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = 10,
                        minimumOrderAmount = 10000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                val coupon2 = couponRepository.save(
                    Coupon.create(
                        name = "쿠폰2",
                        code = "COUPON2",
                        discountType = DiscountType.FIXED,
                        discountValue = 5000,
                        minimumOrderAmount = 20000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )

                couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon1.id))
                couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon2.id))

                // When
                val userCoupons = getCouponQueryUseCase.getUserCoupons(userId)

                // Then
                userCoupons shouldHaveSize 2
            }
        }

        context("사용 가능한 쿠폰만 조회 시") {
            it("ISSUED 상태의 쿠폰만 조회된다") {
                // Given
                val userId = 7000L
                val coupon = couponRepository.save(
                    Coupon.create(
                        name = "사용 가능 쿠폰",
                        code = "USABLE",
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = 15,
                        minimumOrderAmount = 10000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon.id))

                // When
                val availableUserCoupons = getCouponQueryUseCase.getAvailableUserCoupons(userId)

                // Then
                availableUserCoupons shouldHaveSize 1
                availableUserCoupons[0].status shouldBe UserCouponStatus.ISSUED
            }
        }
    }

    describe("쿠폰 사용") {
        context("정상적인 쿠폰 사용 요청일 때") {
            it("쿠폰을 사용하고 할인이 적용된다") {
                // Given
                val userId = 8000L
                val orderAmount = 50000L
                val coupon = couponRepository.save(
                    Coupon.create(
                        name = "10% 할인 쿠폰",
                        code = "DISCOUNT10",
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = 10,
                        minimumOrderAmount = 30000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                val userCoupon = couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon.id))

                // When
                val discountAmount = couponCommandUseCase.applyCoupon(
                    userId = userId,
                    request = UseCouponRequest(userCoupon.id, orderAmount),
                    orderId = 100L
                )

                // Then
                discountAmount shouldBe 5000L // 50,000 * 10% = 5,000

                // 쿠폰 상태 확인
                val usedCoupon = userCouponRepository.findById(userCoupon.id)
                usedCoupon!!.status shouldBe UserCouponStatus.USED
                usedCoupon.usedAt shouldNotBe null
            }
        }

        context("정액 할인 쿠폰 사용 시") {
            it("정액 할인이 적용된다") {
                // Given
                val userId = 9000L
                val orderAmount = 50000L
                val coupon = couponRepository.save(
                    Coupon.create(
                        name = "5천원 할인 쿠폰",
                        code = "FIXED5000",
                        discountType = DiscountType.FIXED,
                        discountValue = 5000,
                        minimumOrderAmount = 20000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                val userCoupon = couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon.id))

                // When
                val discountAmount = couponCommandUseCase.applyCoupon(
                    userId = userId,
                    request = UseCouponRequest(userCoupon.id, orderAmount),
                    orderId = 101L
                )

                // Then
                discountAmount shouldBe 5000L
            }
        }

        context("최소 주문 금액 미달 시") {
            it("예외가 발생한다") {
                // Given
                val userId = 10000L
                val orderAmount = 5000L // 최소 주문 금액보다 적음
                val coupon = couponRepository.save(
                    Coupon.create(
                        name = "최소 주문 금액 테스트",
                        code = "MIN_ORDER",
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = 10,
                        minimumOrderAmount = 10000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                val userCoupon = couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon.id))

                // When & Then
                shouldThrow<CouponException.MinimumOrderAmountNotMet> {
                    couponCommandUseCase.applyCoupon(
                        userId = userId,
                        request = UseCouponRequest(userCoupon.id, orderAmount),
                        orderId = 102L
                    )
                }
            }
        }

        context("이미 사용된 쿠폰을 재사용하려 할 때") {
            it("예외가 발생한다") {
                // Given
                val userId = 11000L
                val orderAmount = 50000L
                val coupon = couponRepository.save(
                    Coupon.create(
                        name = "재사용 방지 테스트",
                        code = "REUSE_TEST",
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = 10,
                        minimumOrderAmount = 10000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                val userCoupon = couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon.id))

                // 첫 번째 사용
                couponCommandUseCase.applyCoupon(userId, UseCouponRequest(userCoupon.id, orderAmount), 103L)

                // When & Then - 재사용 시도
                shouldThrow<CouponException.AlreadyUsedCoupon> {
                    couponCommandUseCase.applyCoupon(userId, UseCouponRequest(userCoupon.id, orderAmount), 104L)
                }
            }
        }
    }

    describe("쿠폰 검증") {
        context("쿠폰 사용 전 검증 시") {
            it("예상 할인 금액을 조회할 수 있다") {
                // Given
                val userId = 12000L
                val orderAmount = 100000L
                val coupon = couponRepository.save(
                    Coupon.create(
                        name = "검증 테스트 쿠폰",
                        code = "VALIDATE",
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = 15,
                        minimumOrderAmount = 50000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now(),
                        validTo = LocalDateTime.now().plusDays(30),
                        createdBy = 1L
                    )
                )
                val userCoupon = couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(coupon.id))

                // When
                val expectedDiscount = validateCouponUseCase.execute(
                    userId = userId,
                    request = UseCouponRequest(userCoupon.id, orderAmount)
                )

                // Then
                expectedDiscount shouldBe 15000L // 100,000 * 15% = 15,000

                // 쿠폰 상태는 변경되지 않음
                val stillUnused = userCouponRepository.findById(userCoupon.id)
                stillUnused!!.status shouldBe UserCouponStatus.ISSUED
            }
        }
    }
})
