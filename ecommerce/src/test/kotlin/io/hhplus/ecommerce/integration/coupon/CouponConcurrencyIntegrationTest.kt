package io.hhplus.ecommerce.integration.coupon

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.coupon.application.CouponIssueService
import io.hhplus.ecommerce.coupon.application.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.presentation.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate

/**
 * 쿠폰 동시성 통합 테스트
 *
 * 선착순 쿠폰 발급 시 동시성 제어를 검증합니다.
 * SADD 원자성 + INCR 카운터 + soldout 플래그 패턴으로 동시성 제어.
 */
class CouponConcurrencyIntegrationTest(
    private val couponCommandUseCase: CouponCommandUseCase,
    private val couponIssueService: CouponIssueService,
    private val couponRepository: CouponRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val cacheManager: CacheManager
) : KotestIntegrationTestBase({

    beforeEach {
        // Redis 전체 초기화 - redisTemplate을 통해 모든 키 삭제
        val keys = redisTemplate.keys("*")
        if (!keys.isNullOrEmpty()) {
            redisTemplate.delete(keys)
        }

        // Caffeine 로컬 캐시 전체 초기화 (COUPON_INFO 등)
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }

    describe("선착순 쿠폰 동시성 제어") {
        context("20명이 10개 한정 쿠폰 동시 발급 시") {
            it("정확히 10명만 발급 성공한다") {
                // Given
                val coupon = Coupon.create(
                    name = "선착순 10명 쿠폰",
                    code = "FIRST10",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 10,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 10,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                )
                val savedCoupon = couponRepository.save(coupon)
                val threadCount = 20

                // When
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            couponCommandUseCase.issueCoupon(
                                userId = 1000L + index,
                                request = IssueCouponRequest(savedCoupon.id)
                            )
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

                // Then
                (successCount.get() + failCount.get()) shouldBe 20
                successCount.get() shouldBe 10
                failCount.get() shouldBe 10
                couponIssueService.getIssuedCount(savedCoupon.id) shouldBe 10
            }
        }

        context("1000명이 100개 한정 쿠폰 동시 발급 시") {
            it("정확히 100명만 발급 성공한다") {
                // Given
                val coupon = Coupon.create(
                    name = "선착순 100명 쿠폰",
                    code = "FIRST100",
                    discountType = DiscountType.FIXED,
                    discountValue = 5000,
                    minimumOrderAmount = 30000L,
                    totalQuantity = 100,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                )
                val savedCoupon = couponRepository.save(coupon)
                val threadCount = 1000

                // When
                val executor = Executors.newFixedThreadPool(100)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            couponCommandUseCase.issueCoupon(
                                userId = 10000L + index,
                                request = IssueCouponRequest(savedCoupon.id)
                            )
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

                // Then
                (successCount.get() + failCount.get()) shouldBe 1000
                successCount.get() shouldBe 100
                failCount.get() shouldBe 900
                couponIssueService.getIssuedCount(savedCoupon.id) shouldBe 100
            }
        }

        context("동일 사용자가 동시에 여러 번 발급 시도 시") {
            it("단 한 번만 발급 성공한다") {
                // Given
                val coupon = Coupon.create(
                    name = "중복 방지 테스트 쿠폰",
                    code = "NODUPE",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 15,
                    minimumOrderAmount = 20000L,
                    totalQuantity = 1000,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                )
                val savedCoupon = couponRepository.save(coupon)
                val sameUserId = 99999L
                val attemptCount = 100

                // When
                val executor = Executors.newFixedThreadPool(50)
                val latch = CountDownLatch(attemptCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(attemptCount) {
                    executor.submit {
                        try {
                            couponCommandUseCase.issueCoupon(
                                userId = sameUserId,
                                request = IssueCouponRequest(savedCoupon.id)
                            )
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

                // Then - SADD 원자성으로 중복 방지 검증
                (successCount.get() + failCount.get()) shouldBe 100
                successCount.get() shouldBe 1
                failCount.get() shouldBe 99
                couponIssueService.isAlreadyIssued(savedCoupon.id, sameUserId) shouldBe true
            }
        }

        context("발급 수량 정밀 제어 검증") {
            it("SADD + INCR + soldout 패턴으로 정확한 수량 제어가 가능하다") {
                // Given
                val coupon = Coupon.create(
                    name = "정밀 제어 테스트 쿠폰",
                    code = "PRECISE50",
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 20,
                    minimumOrderAmount = 10000L,
                    totalQuantity = 50,
                    validFrom = LocalDateTime.now(),
                    validTo = LocalDateTime.now().plusDays(30),
                )
                val savedCoupon = couponRepository.save(coupon)
                val threadCount = 500

                // When
                val executor = Executors.newFixedThreadPool(100)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            couponCommandUseCase.issueCoupon(
                                userId = 50000L + index,
                                request = IssueCouponRequest(savedCoupon.id)
                            )
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            // 실패는 무시
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then
                successCount.get() shouldBe 50
                couponIssueService.getIssuedCount(savedCoupon.id) shouldBe 50
                couponIssueService.getPendingCount(savedCoupon.id) shouldBeLessThanOrEqual 50
            }
        }
    }
})
