package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.hhplus.ecommerce.point.application.usecase.UsePointUseCase
import io.hhplus.ecommerce.point.application.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual as intShouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 포인트 동시성 통합 테스트
 *
 * TestContainers MySQL을 사용하여 포인트 동시 처리 정합성을 검증합니다.
 * - 동시 포인트 사용 (낙관적 락 또는 비관적 락)
 * - 동시 포인트 적립
 * - 동시 포인트 사용 시 잔액 부족 처리
 */
class PointConcurrencyIntegrationTest(
    private val chargePointUseCase: ChargePointUseCase,
    private val usePointUseCase: UsePointUseCase,
    private val getPointQueryUseCase: GetPointQueryUseCase
) : KotestIntegrationTestBase({

    describe("포인트 동시성 제어") {
        context("동시에 포인트를 사용할 때") {
            it("정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val initialAmount = PointAmount(10_000)
                val useAmount = PointAmount(1_000)
                val threadCount = 10

                // 사용자 포인트 생성 및 초기 적립
                chargePointUseCase.execute(userId, initialAmount.value)

                // When - 10개 스레드가 동시에 1,000원씩 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            usePointUseCase.execute(userId, useAmount.value)
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

                // Then - 동시성 제어로 데이터 정합성이 보장되어야 함 (일부 실패 가능)
                val totalOperations = successCount.get() + failCount.get()
                totalOperations shouldBe 10

                // 최소 일부 작업은 성공해야 하고, 데이터 정합성은 보장되어야 함
                successCount.get() intShouldBeGreaterThan 0
                successCount.get() intShouldBeLessThanOrEqual 10

                // 최종 잔액은 성공한 작업만큼만 차감되어야 함
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint shouldNotBe null
                val expectedBalance = 10_000L - (successCount.get() * 1_000L)
                finalUserPoint!!.balance.value shouldBe expectedBalance
            }
        }

        context("동시 사용 시 잔액이 부족할 때") {
            it("일부만 성공한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val initialAmount = PointAmount(5_000)
                val useAmount = PointAmount(1_000)
                val threadCount = 10 // 5,000원인데 10개 스레드가 1,000원씩 사용 시도

                // 사용자 포인트 생성 및 초기 적립
                chargePointUseCase.execute(userId, initialAmount.value)

                // When - 10개 스레드가 동시에 1,000원씩 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            usePointUseCase.execute(userId, useAmount.value)
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

                // Then - 잔액 제약으로 인해 최대 5번만 성공 가능
                val totalOperations = successCount.get() + failCount.get()
                totalOperations shouldBe 10

                // 잔액 제약으로 인해 최대 5번만 성공 가능
                successCount.get() intShouldBeLessThanOrEqual 5
                successCount.get() intShouldBeGreaterThan 0

                // 최종 잔액은 성공한 작업만큼 차감되어야 함
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint shouldNotBe null
                val expectedBalance = 5_000L - (successCount.get() * 1_000L)
                finalUserPoint!!.balance.value shouldBe expectedBalance
            }
        }

        context("동시에 포인트를 적립할 때") {
            it("정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val earnAmount = PointAmount(1_000)
                val threadCount = 20

                // 사용자 포인트 생성

                // When - 20개 스레드가 동시에 1,000원씩 적립
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            chargePointUseCase.execute(userId, earnAmount.value)
                            successCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 적립은 잔액 부족 제약이 없으므로 높은 성공률 기대
                successCount.get() intShouldBeGreaterThan 0 // 적어도 일부는 성공
                successCount.get() intShouldBeLessThanOrEqual 20

                // 최종 잔액은 성공한 작업만큼 적립되어야 함
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint shouldNotBe null
                val expectedBalance = successCount.get() * 1_000L
                finalUserPoint!!.balance.value shouldBe expectedBalance
            }
        }

        context("동시에 포인트 적립과 사용을 할 때") {
            it("정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val initialAmount = PointAmount(5_000)
                val earnAmount = PointAmount(1_000)
                val useAmount = PointAmount(500)
                val threadCount = 20 // 10번 적립, 10번 사용

                // 사용자 포인트 생성 및 초기 적립
                chargePointUseCase.execute(userId, initialAmount.value)

                // When - 10개 스레드는 적립, 10개 스레드는 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val earnSuccessCount = AtomicInteger(0)
                val useSuccessCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            if (index % 2 == 0) {
                                // 짝수 인덱스: 적립
                                chargePointUseCase.execute(userId, earnAmount.value)
                                earnSuccessCount.incrementAndGet()
                            } else {
                                // 홀수 인덱스: 사용
                                usePointUseCase.execute(userId, useAmount.value)
                                useSuccessCount.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            // 잔액 부족으로 실패할 수 있음
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 동시성 제어로 일부 작업은 성공해야 함
                earnSuccessCount.get() intShouldBeLessThanOrEqual 10
                useSuccessCount.get() intShouldBeLessThanOrEqual 10

                // 적어도 일부 작업은 성공해야 함
                val totalSuccessOperations = earnSuccessCount.get() + useSuccessCount.get()
                totalSuccessOperations intShouldBeGreaterThan 0

                // 최종 잔액 확인 (정확한 계산)
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint shouldNotBe null

                // 최종 잔액 = 초기 + (적립 성공 횟수 * 1000) - (사용 성공 횟수 * 500)
                val expectedBalance = 5_000 + (earnSuccessCount.get() * 1_000) - (useSuccessCount.get() * 500)
                finalUserPoint!!.balance.value shouldBe expectedBalance.toLong()
            }
        }

        context("높은 동시성 환경에서") {
            it("포인트 정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val initialAmount = PointAmount(100_000)
                val useAmount = PointAmount(100)
                val threadCount = 100

                // 사용자 포인트 생성 및 초기 적립
                chargePointUseCase.execute(userId, initialAmount.value)

                // When - 100개 스레드가 동시에 100원씩 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            usePointUseCase.execute(userId, useAmount.value)
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            // 동시성 제어 실패 또는 잔액 부족
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 높은 동시성에서도 데이터 정합성 보장 (성공률은 락 경합에 따라 변동)
                successCount.get() intShouldBeGreaterThan 0 // 적어도 1건은 성공
                successCount.get() intShouldBeLessThanOrEqual threadCount

                // 최종 잔액은 성공한 작업만큼 차감되어야 함
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint shouldNotBe null
                val expectedBalance = 100_000L - (successCount.get() * 100L)
                finalUserPoint!!.balance.value shouldBe expectedBalance
            }
        }
    }
})
