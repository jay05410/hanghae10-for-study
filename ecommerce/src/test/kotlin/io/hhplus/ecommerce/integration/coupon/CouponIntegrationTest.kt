package io.hhplus.ecommerce.integration.coupon

import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.coupon.application.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.presentation.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.exception.CouponException
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

/**
 * 쿠폰 발급 비즈니스 로직 통합 테스트
 *
 * 검증 사항:
 * 1. 쿠폰 발급 성공 케이스
 * 2. 쿠폰 존재 여부 검증
 * 3. 중복 발급 방지 로직
 * 4. 품절 쿠폰 발급 시도
 * 5. 예외 처리 로직
 */
class CouponIntegrationTest(
    private val couponDomainService: CouponDomainService,
    private val couponRepository: CouponRepository,
    private val couponCommandUseCase: CouponCommandUseCase
) : KotestIntegrationTestBase({
        describe("쿠폰 발급 비즈니스 로직") {
            context("유효한 쿠폰 발급 요청 시") {
                it("쿠폰이 성공적으로 발급된다") {
                    // Given: 쿠폰 생성
                    val coupon = Coupon.create(
                        name = "테스트 쿠폰",
                        code = "TEST100",
                        discountType = DiscountType.FIXED,
                        discountValue = 1000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now().minusDays(1),
                        validTo = LocalDateTime.now().plusDays(30)
                    )
                    val savedCoupon = couponRepository.save(coupon)
                    val userId = 1L

                    // When: 쿠폰 발급
                    val userCoupon = couponDomainService.issueCoupon(savedCoupon, userId)

                    // Then: 쿠폰 발급 확인
                    userCoupon.userId shouldBe userId
                    userCoupon.couponId shouldBe savedCoupon.id
                    userCoupon.status.name shouldBe "ISSUED"

                    // 쿠폰 발급 수량 증가 확인
                    val updatedCoupon = couponRepository.findById(savedCoupon.id)
                    updatedCoupon shouldNotBe null
                    updatedCoupon!!.issuedQuantity shouldBe 1
                }
            }

            context("존재하지 않는 쿠폰 발급 요청 시") {
                it("CouponNotFound 예외가 발생한다") {
                    // Given: 존재하지 않는 쿠폰 ID
                    val nonExistentCouponId = 99999L
                    val userId = 2L

                    // When & Then: 존재하지 않는 쿠폰 발급 시 예외 발생
                    val exception = shouldThrow<CouponException.CouponNotFound> {
                        couponCommandUseCase.issueCoupon(userId, IssueCouponRequest(nonExistentCouponId))
                    }

                    exception.data["couponId"] shouldBe nonExistentCouponId
                }
            }

            context("동일한 사용자가 같은 쿠폰을 중복 발급받으려 하면") {
                it("AlreadyIssuedCoupon 예외가 발생한다") {
                    // Given: 쿠폰 생성
                    val coupon = Coupon.create(
                        name = "중복 테스트 쿠폰",
                        code = "DUP100",
                        discountType = DiscountType.FIXED,
                        discountValue = 1000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now().minusDays(1),
                        validTo = LocalDateTime.now().plusDays(30)
                    )
                    val savedCoupon = couponRepository.save(coupon)
                    val userId = 3L

                    // When: 첫 번째 쿠폰 발급
                    couponDomainService.issueCoupon(savedCoupon, userId)

                    // Then: 두 번째 쿠폰 발급 시도 시 예외 발생
                    val exception = shouldThrow<CouponException.AlreadyIssuedCoupon> {
                        couponDomainService.issueCoupon(savedCoupon, userId)
                    }

                    exception.data["userId"] shouldBe userId
                    exception.data["couponName"] shouldBe "중복 테스트 쿠폰"
                }
            }

            context("품절된 쿠폰 발급 요청 시") {
                it("CouponSoldOut 예외가 발생한다") {
                    // Given: 1개 한정 쿠폰 생성 및 발급
                    val coupon = Coupon.create(
                        name = "한정 쿠폰",
                        code = "LIMITED1",
                        discountType = DiscountType.FIXED,
                        discountValue = 1000L,
                        totalQuantity = 1,
                        validFrom = LocalDateTime.now().minusDays(1),
                        validTo = LocalDateTime.now().plusDays(30)
                    )
                    val savedCoupon = couponRepository.save(coupon)

                    // 첫 번째 사용자가 쿠폰 발급받음 (품절)
                    couponDomainService.issueCoupon(savedCoupon, 1L)

                    // When & Then: 두 번째 사용자 발급 시도 시 예외 발생
                    val exception = shouldThrow<CouponException.CouponSoldOut> {
                        couponDomainService.issueCoupon(savedCoupon, 2L)
                    }

                    exception.data["couponName"] shouldBe "한정 쿠폰"
                    exception.data["remainingQuantity"] shouldBe 0
                }
            }
        }
})