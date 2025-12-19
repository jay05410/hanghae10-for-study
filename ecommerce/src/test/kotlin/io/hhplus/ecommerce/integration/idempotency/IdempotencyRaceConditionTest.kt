package io.hhplus.ecommerce.integration.idempotency

import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.common.idempotency.IdempotencyService
import io.hhplus.ecommerce.support.ConcurrentTestHelper
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.kotest.matchers.shouldBe
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.atomic.AtomicInteger

/**
 * 멱등성 서비스 동시성 테스트
 *
 * tryAcquire()의 원자적 SETNX 기반 멱등성 보장을 검증
 * 재고/포인트 처리 등에서 사용되는 멱등성 체크 로직 검증
 */
class IdempotencyRaceConditionTest(
    private val idempotencyService: IdempotencyService,
    private val redisTemplate: RedisTemplate<String, Any>
) : KotestIntegrationTestBase({

    beforeTest {
        // 테스트 키 정리
        redisTemplate.keys("ecom:inv:deducted:*")?.let { keys ->
            if (keys.isNotEmpty()) redisTemplate.delete(keys)
        }
    }

    describe("IdempotencyService 동시성 테스트") {

        context("tryAcquire 방식 (원자적 SETNX)") {

            it("동시 요청 시 중복 처리가 완전히 방지됨") {
                // given
                val orderId = 10L
                val idempotencyKey = RedisKeyNames.Inventory.deductedKey(orderId)
                val processedCount = AtomicInteger(0)

                // when: 10개 스레드가 동시에 처리 시도
                val result = ConcurrentTestHelper.execute(
                    threadCount = 10,
                    poolSize = 10
                ) {
                    if (idempotencyService.tryAcquire(idempotencyKey)) {
                        Thread.sleep(5)
                        processedCount.incrementAndGet()
                    }
                }

                result.printSummary()
                println("실제 처리된 횟수 (tryAcquire): ${processedCount.get()}")

                // then: 정확히 1번만 처리됨
                processedCount.get() shouldBe 1
            }
        }

        context("대량 동시 요청 (100개 스레드)") {

            it("대량 동시 요청에서도 정확히 1회만 처리") {
                // given
                val orderId = 100L
                val idempotencyKey = RedisKeyNames.Inventory.deductedKey(orderId)
                val processedCount = AtomicInteger(0)

                // when: 100개 스레드가 동시에 처리 시도
                val result = ConcurrentTestHelper.execute(
                    threadCount = 100,
                    poolSize = 50
                ) {
                    if (idempotencyService.tryAcquire(idempotencyKey)) {
                        Thread.sleep(2)
                        processedCount.incrementAndGet()
                    }
                }

                result.printSummary()
                println("실제 처리된 횟수 (100 스레드): ${processedCount.get()}")

                // then: 정확히 1번만 처리됨
                processedCount.get() shouldBe 1
            }
        }
    }
})
