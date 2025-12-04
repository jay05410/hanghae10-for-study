package io.hhplus.ecommerce.integration.coupon

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.coupon.application.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.presentation.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.data.redis.core.RedisTemplate
/**
 * 쿠폰 동시성 통합 테스트
 *
 * 선착순 쿠폰 발급 시 동시성 제어를 검증합니다.
 */
class CouponConcurrencyIntegrationTest(
    private val couponCommandUseCase: CouponCommandUseCase,
    private val couponRepository: CouponRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) : KotestIntegrationTestBase({

    beforeEach {
        // Redis 데이터 초기화
        redisTemplate.execute { connection ->
            connection.serverCommands().flushAll()
            null
        }
    }

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

                // Then - @DistributedLock으로 인한 동시성 제어 검증
                (successCount.get() + failCount.get()) shouldBe 20 // 총 시도 수는 20개
                successCount.get() shouldBe 10 // 큐에는 10명만 등록 성공
                failCount.get() shouldBe 10 // 나머지 10명은 큐 등록 실패

                // 분산락 효과 검증 - Race Condition 없이 정확히 10개만 큐에 등록됨
                // @DistributedLock(key = "coupon:issue:#{#request.couponId}")가
                // 동일 쿠폰에 대한 동시 접근을 직렬화하여 큐 크기 검증을 원자적으로 수행
            }
        }
    }
})
