package io.hhplus.tdd.point

import io.hhplus.tdd.common.response.ApiResponse
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Point API 통합 테스트 (Kotest BehaviorSpec)
 *
 * Controller + Service + Database 전체 흐름을 검증.
 */
class PointApiIntegrationSpec : BehaviorSpec({

    // ===== 포인트 조회 API 통합 테스트 =====

    Given("사용자 ID 100의 포인트가 25,000원으로 설정되어 있을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 100L
        val expectedPoint = 25000L
        userPointTable.insertOrUpdate(userId, expectedPoint)

        When("GET /point/{id} 요청을 하면") {
            val response = pointController.point(userId)

            Then("성공 응답과 포인트 정보가 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.id shouldBe userId
                response.data.point shouldBe expectedPoint
                response.data.updateMillis shouldBeGreaterThan 0
            }
        }
    }

    Given("존재하지 않는 사용자 ID 999가 주어졌을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 999L

        When("GET /point/{id} 요청을 하면") {
            val response = pointController.point(userId)

            Then("성공 응답과 0원 포인트가 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.id shouldBe userId
                response.data.point shouldBe 0L
                response.data.updateMillis shouldBeGreaterThan 0
            }
        }
    }

    Given("사용자 ID 101의 포인트가 업데이트 되었을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 101L
        val initialPoint = 10000L
        val updatedPoint = 15000L

        userPointTable.insertOrUpdate(userId, initialPoint)
        userPointTable.insertOrUpdate(userId, updatedPoint)

        When("GET /point/{id} 요청을 하면") {
            val response = pointController.point(userId)

            Then("업데이트된 포인트가 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.point shouldBe updatedPoint
            }
        }
    }

    Given("사용자 ID 102의 포인트가 15,000원일 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 102L
        val expectedPoint = 15000L
        userPointTable.insertOrUpdate(userId, expectedPoint)

        When("여러 번 조회해도") {
            val response1 = pointController.point(userId)
            val response2 = pointController.point(userId)
            val response3 = pointController.point(userId)

            Then("동일한 포인트가 반환되어야 한다") {
                response1.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response2.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response3.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()

                response1.data.point shouldBe expectedPoint
                response2.data.point shouldBe expectedPoint
                response3.data.point shouldBe expectedPoint
            }
        }
    }

    // ===== 포인트 충전 API 통합 테스트 =====

    Given("사용자 ID 110이 현재 5,000원을 보유하고 있을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 110L
        val initialPoint = 5000L
        val chargeAmount = 3000L

        userPointTable.insertOrUpdate(userId, initialPoint)

        When("PATCH /point/{id}/charge로 3,000원을 충전하면") {
            val response = pointController.charge(userId, chargeAmount)

            Then("성공 응답과 8,000원 포인트가 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.id shouldBe userId
                response.data.point shouldBe (initialPoint + chargeAmount)
            }
        }
    }

    Given("사용자 ID 111이 1,000원과 2,000원을 순차적으로 충전할 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 111L
        val initialPoint = 5000L

        userPointTable.insertOrUpdate(userId, initialPoint)

        When("순차적으로 충전하면") {
            pointController.charge(userId, 1000L)
            pointController.charge(userId, 2000L)

            val response = pointController.point(userId)

            Then("최종 포인트는 8,000원이 되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.point shouldBe (initialPoint + 1000L + 2000L)
            }
        }
    }

    Given("사용자 ID 112가 5,000원 충전 후 내역을 조회할 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 112L
        val chargeAmount = 5000L

        userPointTable.insertOrUpdate(userId, 10000L)

        When("충전하고 내역을 조회하면") {
            pointController.charge(userId, chargeAmount)
            val response = pointController.history(userId)

            Then("충전 내역이 존재해야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<List<PointHistory>>>()
                response.data shouldHaveSize 1
                response.data[0].type shouldBe TransactionType.CHARGE
                response.data[0].amount shouldBe chargeAmount
            }
        }
    }

    // ===== 포인트 사용 API 통합 테스트 =====

    Given("사용자 ID 120이 10,000원을 보유하고 있을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 120L
        val initialPoint = 10000L
        val useAmount = 3000L

        userPointTable.insertOrUpdate(userId, initialPoint)

        When("PATCH /point/{id}/use로 3,000원을 사용하면") {
            val response = pointController.use(userId, useAmount)

            Then("성공 응답과 7,000원 포인트가 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.point shouldBe (initialPoint - useAmount)
            }
        }
    }

    Given("사용자 ID 121이 10,000원을 보유하고 2,000원과 3,000원을 순차 사용할 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 121L
        val initialPoint = 10000L

        userPointTable.insertOrUpdate(userId, initialPoint)

        When("순차적으로 사용하면") {
            pointController.use(userId, 2000L)
            pointController.use(userId, 3000L)

            val response = pointController.point(userId)

            Then("최종 포인트는 5,000원이 되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<UserPoint>>()
                response.data.point shouldBe (initialPoint - 2000L - 3000L)
            }
        }
    }

    // ===== 포인트 내역 조회 API 통합 테스트 =====

    Given("사용자 ID 130이 충전/사용 내역을 가지고 있을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 130L

        userPointTable.insertOrUpdate(userId, 20000L)
        pointController.charge(userId, 5000L)
        pointController.use(userId, 2000L)
        pointController.charge(userId, 3000L)

        When("GET /point/{id}/histories 요청을 하면") {
            val response = pointController.history(userId)

            Then("성공 응답과 3건의 내역이 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<List<PointHistory>>>()
                response.data shouldHaveSize 3
                // 최신순 정렬 확인 (같을 수도 가능)
                response.data[0].timeMillis shouldBeGreaterThanOrEqual response.data[1].timeMillis
                response.data[1].timeMillis shouldBeGreaterThanOrEqual response.data[2].timeMillis
            }
        }
    }

    Given("거래 내역이 없는 사용자 ID 131이 주어졌을 때") {
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val pointService = PointService(userPointTable, pointHistoryTable)
        val pointController = PointController(pointService)

        val userId = 131L

        When("GET /point/{id}/histories 요청을 하면") {
            val response = pointController.history(userId)

            Then("성공 응답과 빈 목록이 반환되어야 한다") {
                response.shouldBeInstanceOf<ApiResponse.Success<List<PointHistory>>>()
                response.data shouldHaveSize 0
            }
        }
    }
})
