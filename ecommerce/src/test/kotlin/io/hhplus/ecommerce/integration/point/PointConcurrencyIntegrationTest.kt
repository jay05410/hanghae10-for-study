package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
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
    private val pointService: PointService,
    private val userPointRepository: UserPointRepository
) : KotestIntegrationTestBase({

    describe("포인트 동시성 제어") {
        context("동시에 포인트를 사용할 때") {
            it("정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val initialAmount = PointAmount(10_000)
                val useAmount = PointAmount(1_000)
                val threadCount = 10
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 10개 스레드가 동시에 1,000원씩 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            pointService.usePoint(userId, useAmount, createdBy)
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

                // Then - 정확히 10번 모두 성공해야 함 (10,000원 / 1,000원 = 10번)
                successCount.get() shouldBe 10
                failCount.get() shouldBe 0

                // 최종 잔액 확인
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 0L // 10,000 - (1,000 * 10)
            }
        }

        context("동시 사용 시 잔액이 부족할 때") {
            it("일부만 성공한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val initialAmount = PointAmount(5_000)
                val useAmount = PointAmount(1_000)
                val threadCount = 10 // 5,000원인데 10개 스레드가 1,000원씩 사용 시도
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 10개 스레드가 동시에 1,000원씩 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            pointService.usePoint(userId, useAmount, createdBy)
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

                // Then - 5번 성공, 5번 실패
                successCount.get() shouldBe 5
                failCount.get() shouldBe 5

                // 최종 잔액 확인
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 0L // 5,000 - (1,000 * 5)
            }
        }

        context("동시에 포인트를 적립할 때") {
            it("정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val earnAmount = PointAmount(1_000)
                val threadCount = 20
                val createdBy = userId

                // 사용자 포인트 생성
                pointService.createUserPoint(userId, createdBy)

                // When - 20개 스레드가 동시에 1,000원씩 적립
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            pointService.earnPoint(userId, earnAmount, createdBy)
                            successCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 모두 성공
                successCount.get() shouldBe 20

                // 최종 잔액 확인
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 20_000L // 1,000 * 20
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
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

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
                                pointService.earnPoint(userId, earnAmount, createdBy)
                                earnSuccessCount.incrementAndGet()
                            } else {
                                // 홀수 인덱스: 사용
                                pointService.usePoint(userId, useAmount, createdBy)
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

                // Then - 적립은 모두 성공해야 함
                earnSuccessCount.get() shouldBe 10

                // 최종 잔액 확인 (정확한 계산)
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null

                // 최종 잔액 = 초기 + (적립 * 10) - (사용 성공 횟수 * 500)
                val expectedBalance = 5_000 + (1_000 * 10) - (useSuccessCount.get() * 500)
                finalUserPoint!!.balance.value shouldBe expectedBalance.toLong()
                useSuccessCount.get().toLong() shouldBeLessThanOrEqual 10 // 최대 10번 사용 가능
            }
        }

        context("높은 동시성 환경에서") {
            it("포인트 정합성이 보장된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val initialAmount = PointAmount(100_000)
                val useAmount = PointAmount(100)
                val threadCount = 100
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 100개 스레드가 동시에 100원씩 사용
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            pointService.usePoint(userId, useAmount, createdBy)
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

                // Then
                successCount.get() shouldBe threadCount

                // 최종 잔액 확인
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 90_000L // 100,000 - (100 * 100)
            }
        }
    }
})
