package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.service.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture

/**
 * 포인트 시스템 동시성 제어 통합 테스트
 *
 * 동시성 이슈 제어 및 검증
 */
class PointConcurrencyIntegrationTest : DescribeSpec({

    describe("포인트 동시성 제어") {
        context("동시 충전 요청") {
            it("100개 스레드가 동시에 1,000원씩 충전하면 정확히 100,000원이 되어야 함") {
                // 실제 컴포넌트 사용
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)

                val userId = 1L
                val chargeAmount = 1000L
                val threadCount = 100

                // 100개 스레드에서 동시 충전 실행
                val futures = (1..threadCount).map {
                    CompletableFuture.supplyAsync {
                        chargeService.charge(userId, chargeAmount)
                    }
                }

                // 모든 스레드 완료 대기
                CompletableFuture.allOf(*futures.toTypedArray()).get()

                // 최종 포인트 검증
                val finalPoint = userPointTable.selectById(userId)
                finalPoint.point shouldBe (threadCount * chargeAmount) // 100,000원

                // 히스토리 개수 검증
                val histories = historyTable.selectAllByUserId(userId)
                histories.size shouldBe threadCount
            }

            it("동일 사용자에 대한 동시 충전과 다른 사용자 충전이 독립적으로 처리되어야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)

                val user1Id = 1L
                val user2Id = 2L
                val chargeAmount = 1000L

                // 사용자1: 50번 충전, 사용자2: 30번 충전을 동시 실행
                val user1Futures = (1..50).map {
                    CompletableFuture.supplyAsync { chargeService.charge(user1Id, chargeAmount) }
                }
                val user2Futures = (1..30).map {
                    CompletableFuture.supplyAsync { chargeService.charge(user2Id, chargeAmount) }
                }

                val allFutures = user1Futures + user2Futures
                CompletableFuture.allOf(*allFutures.toTypedArray()).get()

                // 각 사용자별 포인트 검증
                val user1Point = userPointTable.selectById(user1Id)
                val user2Point = userPointTable.selectById(user2Id)

                user1Point.point shouldBe 50000L // 50 * 1000
                user2Point.point shouldBe 30000L // 30 * 1000
            }
        }

        context("동시 사용 요청") {
            it("잔고 부족 상황에서 동시 사용 요청 시 일부만 성공해야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val queryService = PointQueryService(userPointTable, historyTable)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)

                val userId = 1L
                val initialPoint = 10000L  // 10,000원으로 제한
                val useAmount = 1000L      // 1,000원씩 사용
                val threadCount = 50       // 50개 동시 요청

                userPointTable.insertOrUpdate(userId, initialPoint)

                // 50개 스레드에서 동시에 1,000원씩 사용 (10개만 성공 가능)
                val futures = (1..threadCount).map {
                    CompletableFuture.supplyAsync {
                        try {
                            useService.use(userId, useAmount)
                            true // 성공
                        } catch (e: Exception) {
                            false // 실패 (잔고 부족 등)
                        }
                    }
                }

                CompletableFuture.allOf(*futures.toTypedArray()).get()
                val successResults = futures.map { it.get() }
                val successCount = successResults.count { it }
                val failureCount = successResults.count { !it }

                println("성공한 요청: $successCount, 실패한 요청: $failureCount")
                println("최종 포인트: ${userPointTable.selectById(userId).point}")

                // 검증: 정확히 10개만 성공해야 함 (잔고가 10,000원이고 1,000원씩 사용)
                successCount shouldBe 10
                failureCount shouldBe 40

                // 최종 포인트 검증: 0원이어야 함
                val finalPoint = userPointTable.selectById(userId)
                finalPoint.point shouldBe 0L

                // 히스토리 개수 검증: 성공한 거래만 기록됨
                val histories = historyTable.selectAllByUserId(userId)
                histories.size shouldBe successCount
            }
        }

        context("충전과 사용 혼합 동시 요청") {
            it("충전과 사용이 동시에 실행되어도 정확한 잔고가 유지되어야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val queryService = PointQueryService(userPointTable, historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)

                val userId = 1L
                val initialPoint = 50000L
                userPointTable.insertOrUpdate(userId, initialPoint)

                // 30개의 충전(+1000원)과 20개의 사용(-500원)을 동시 실행
                val chargeFutures = (1..30).map {
                    CompletableFuture.supplyAsync { chargeService.charge(userId, 1000L) }
                }
                val useFutures = (1..20).map {
                    CompletableFuture.supplyAsync { useService.use(userId, 500L) }
                }

                val allFutures = chargeFutures + useFutures
                CompletableFuture.allOf(*allFutures.toTypedArray()).get()

                // 최종 포인트 검증: 50000 + (30 * 1000) - (20 * 500) = 70000
                val finalPoint = userPointTable.selectById(userId)
                finalPoint.point shouldBe 70000L

                // 히스토리 개수 검증
                val histories = historyTable.selectAllByUserId(userId)
                histories.size shouldBe 50 // 30개 충전 + 20개 사용
            }
        }

        context("사용자별 락 검증") {
            it("같은 사용자의 동시 요청은 순차 처리되지만 다른 사용자는 병렬 처리되어야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)

                val user1Id = 1L
                val user2Id = 2L
                val user3Id = 3L

                // 각 사용자에게 10번씩 동시 충전
                val allFutures = listOf(
                    (1..10).map { CompletableFuture.supplyAsync { chargeService.charge(user1Id, 1000L) } },
                    (1..10).map { CompletableFuture.supplyAsync { chargeService.charge(user2Id, 2000L) } },
                    (1..10).map { CompletableFuture.supplyAsync { chargeService.charge(user3Id, 3000L) } }
                ).flatten()

                CompletableFuture.allOf(*allFutures.toTypedArray()).get()

                // 각 사용자별 최종 포인트 검증
                userPointTable.selectById(user1Id).point shouldBe 10000L // 10 * 1000
                userPointTable.selectById(user2Id).point shouldBe 20000L // 10 * 2000
                userPointTable.selectById(user3Id).point shouldBe 30000L // 10 * 3000

                // 각 사용자별 내역 개수 검증
                historyTable.selectAllByUserId(user1Id).size shouldBe 10
                historyTable.selectAllByUserId(user2Id).size shouldBe 10
                historyTable.selectAllByUserId(user3Id).size shouldBe 10
            }
        }
    }
})