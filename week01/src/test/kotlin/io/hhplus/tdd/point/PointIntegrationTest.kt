package io.hhplus.tdd.point

import io.hhplus.tdd.common.response.ApiResponse
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.service.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * 포인트 시스템 통합 테스트
 *
 * Controller → Service → Database 전체 플로우 검증
 * 각 기능별(조회/충전/사용/내역) 통합 테스트
 */
class PointIntegrationTest : DescribeSpec({

    describe("포인트 조회 통합 테스트") {
        context("사용자 포인트가 설정되어 있을 때") {
            it("Controller → Service → Database 전체 플로우가 정상 동작해야 함") {
                // 실제 컴포넌트 구성
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 1L
                val expectedPoint = 15000L
                userPointTable.insertOrUpdate(userId, expectedPoint)

                // 전체 플로우 실행
                val response = controller.point(userId)

                // 검증
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.id shouldBe userId
                response.data.point shouldBe expectedPoint
            }
        }

        context("존재하지 않는 사용자일 때") {
            it("0포인트로 초기화된 UserPoint를 반환해야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 999L

                val response = controller.point(userId)

                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.id shouldBe userId
                response.data.point shouldBe 0L
            }
        }
    }

    describe("포인트 충전 통합 테스트") {
        context("정상적인 충전 요청일 때") {
            it("Controller → Service → Database 전체 플로우가 정상 동작해야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 1L
                val initialPoint = 10000L
                val chargeAmount = 5000L
                userPointTable.insertOrUpdate(userId, initialPoint)

                // 충전 실행
                val response = controller.charge(userId, chargeAmount)

                // 검증
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.point shouldBe (initialPoint + chargeAmount)

                // 내역 확인
                val histories = historyTable.selectAllByUserId(userId)
                histories shouldHaveSize 1
                histories[0].type shouldBe TransactionType.CHARGE
                histories[0].amount shouldBe chargeAmount
            }
        }

        context("연속 충전일 때") {
            it("누적 포인트가 정확히 계산되어야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 1L
                userPointTable.insertOrUpdate(userId, 5000L)

                // 연속 충전
                controller.charge(userId, 2000L)
                controller.charge(userId, 3000L)
                val finalResponse = controller.point(userId)

                finalResponse.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                finalResponse.data.point shouldBe 10000L // 5000 + 2000 + 3000

                // 내역 확인
                val histories = historyTable.selectAllByUserId(userId)
                histories shouldHaveSize 2
            }
        }
    }

    describe("포인트 사용 통합 테스트") {
        context("정상적인 사용 요청일 때") {
            it("Controller → Service → Database 전체 플로우가 정상 동작해야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 1L
                val initialPoint = 10000L
                val useAmount = 3000L
                userPointTable.insertOrUpdate(userId, initialPoint)

                // 사용 실행
                val response = controller.use(userId, useAmount)

                // 검증
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.point shouldBe (initialPoint - useAmount)

                // 내역 확인
                val histories = historyTable.selectAllByUserId(userId)
                histories shouldHaveSize 1
                histories[0].type shouldBe TransactionType.USE
                histories[0].amount shouldBe useAmount
            }
        }
    }

    describe("포인트 내역 조회 통합 테스트") {
        context("충전/사용 내역이 있을 때") {
            it("Controller → Service → Database 전체 플로우가 정상 동작해야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 1L
                userPointTable.insertOrUpdate(userId, 20000L)

                // 충전/사용 실행
                controller.charge(userId, 5000L)
                controller.use(userId, 2000L)
                controller.charge(userId, 3000L)

                // 내역 조회
                val response = controller.history(userId)

                // 검증
                response.shouldBeInstanceOf<ApiResponse.Success<List<PointHistory>>>()
                response.data shouldHaveSize 3

                // 최신순 정렬 확인
                val histories = response.data
                // 가장 최근 내역이 첫 번째에 와야 함
                (histories[0].timeMillis >= histories[1].timeMillis) shouldBe true
                (histories[1].timeMillis >= histories[2].timeMillis) shouldBe true
            }
        }

        context("내역이 없을 때") {
            it("빈 목록을 반환해야 함") {
                val userPointTable = UserPointTable()
                val historyTable = PointHistoryTable()
                val queryService = PointQueryService(userPointTable, historyTable)
                val lockManager = UserLockManager()
                val transactionLogger = PointTransactionLogger(historyTable)
                val chargeService = PointChargeService(userPointTable, lockManager, transactionLogger)
                val useService = PointUseService(userPointTable, queryService, lockManager, transactionLogger)
                val controller = PointController(queryService, chargeService, useService)

                val userId = 999L

                val response = controller.history(userId)

                response.shouldBeInstanceOf<ApiResponse.Success<List<PointHistory>>>()
                response.data shouldHaveSize 0
            }
        }
    }
})