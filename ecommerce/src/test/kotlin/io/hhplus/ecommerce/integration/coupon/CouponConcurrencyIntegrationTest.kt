package io.hhplus.ecommerce.integration.coupon

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.coupon.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 쿠폰 동시성 통합 테스트
 *
 * 선착순 쿠폰 발급 시 동시성 제어를 검증합니다.
 */
class CouponConcurrencyIntegrationTest(
    private val couponCommandUseCase: CouponCommandUseCase,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) : KotestIntegrationTestBase({

    describe("선착순 쿠폰 동시성 제어") {
        context("선착순 쿠폰 동시 발급 시") {
            it("정확한 수량만 발급된다") {
                // Given - 10개 한정 쿠폰
                val coupon = Coupon.create(
                    name = "선착순 10명 쿠폰",
                    code = "FIRST10",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 10,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 10,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                    createdBy = 1L
                )
                val savedCoupon = couponRepository.save(coupon)
                val threadCount = 20 // 20명이 동시에 발급 시도

                // When - 20명이 동시에 쿠폰 발급 시도
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            couponCommandUseCase.issueCoupon(userId = 1000L + index, request = IssueCouponRequest(savedCoupon.id))
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 정확히 10개만 발급되어야 함
                successCount.get() shouldBe 10
                failCount.get() shouldBe 10

                // 쿠폰 발급 수량 확인
                val updatedCoupon = couponRepository.findById(savedCoupon.id)
                updatedCoupon shouldNotBe null
                updatedCoupon!!.issuedQuantity shouldBe 10

                // 사용자 쿠폰 발급 확인
                val issuedCoupons = (0 until 20).mapNotNull { index ->
                    userCouponRepository.findByUserIdAndCouponId(1000L + index, savedCoupon.id)
                }
                issuedCoupons.size shouldBe 10
            }
        }
    }
})
