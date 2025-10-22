package io.hhplus.tdd.point

import io.hhplus.tdd.common.exception.point.PointException
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*

/**
 * PointService 단위 테스트 (Kotest BehaviorSpec)
 *
 * Mock을 사용하여 PointService의 비즈니스 로직만 검증.
 *
 * 테스트 구조는 BUSINESS_POLICIES.md의 정책을 반영합니다:
 * - 1. 포인트 충전 정책 (1.1 최소 금액, 1.2 최대 금액, 1.3 충전 단위)
 * - 2. 포인트 사용 정책 (2.1 잔고 부족, 2.2 사용 단위, 2.3 일일 한도, 2.4 최소 사용 금액)
 * - 3. 포인트 조회 정책 (3.1 사용자 존재 검증)
 * - 4. 포인트 내역 조회 정책 (4.1 내역 제한, 4.2 시간 정렬)
 */
class PointServiceSpec : BehaviorSpec({

    // =============================================================================
    // 3. 포인트 조회 정책
    // =============================================================================

    // ----- 3.1 사용자 존재 검증 -----

    Given("[정책 3.1] 사용자 ID 1과 포인트 10,000원이 존재할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val expectedPoint = 10000L
        val stubUserPoint = UserPoint(userId, expectedPoint, System.currentTimeMillis())
        every { userPointTable.selectById(userId) } returns stubUserPoint

        When("포인트를 조회하면") {
            val result = pointService.getPoint(userId)

            Then("조회된 포인트는 10,000원이어야 한다") {
                result.id shouldBe userId
                result.point shouldBe expectedPoint
                verify(exactly = 1) { userPointTable.selectById(userId) }
            }
        }
    }

    Given("[정책 3.1] 존재하지 않는 사용자 ID 999가 주어졌을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 999L
        val stubUserPoint = UserPoint(userId, 0L, System.currentTimeMillis())
        every { userPointTable.selectById(userId) } returns stubUserPoint

        When("포인트를 조회하면") {
            val result = pointService.getPoint(userId)

            Then("포인트는 0원이어야 한다") {
                result.id shouldBe userId
                result.point shouldBe 0L
                verify(exactly = 1) { userPointTable.selectById(userId) }
            }
        }
    }

    // =============================================================================
    // 1. 포인트 충전 정책
    // =============================================================================

    // ----- 기본 충전 성공 시나리오 -----

    Given("[기본 기능] 사용자 ID 1, 현재 포인트 10,000원, 충전 금액 5,000원이 주어졌을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val currentPoint = 10000L
        val chargeAmount = 5000L
        val expectedNewPoint = currentPoint + chargeAmount

        val currentUserPoint = UserPoint(userId, currentPoint, System.currentTimeMillis())
        val newUserPoint = UserPoint(userId, expectedNewPoint, System.currentTimeMillis())

        every { userPointTable.selectById(userId) } returns currentUserPoint
        every { userPointTable.insertOrUpdate(userId, expectedNewPoint) } returns newUserPoint
        every { pointHistoryTable.insert(any(), any(), any(), any()) } returns mockk(relaxed = true)

        When("5,000원을 충전하면") {
            val result = pointService.charge(userId, chargeAmount)

            Then("포인트는 15,000원이 되어야 한다") {
                result.point shouldBe expectedNewPoint
                verify(exactly = 1) { userPointTable.selectById(userId) }
                verify(exactly = 1) { userPointTable.insertOrUpdate(userId, expectedNewPoint) }
                verify(exactly = 1) {
                    pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, any())
                }
            }
        }
    }

    // ----- 1.1 최소 충전 금액 제한 (1,000원 이상) -----

    Given("[정책 1.1] 999원을 충전하려고 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val invalidAmount = 999L

        When("충전을 시도하면") {
            Then("MinimumChargeAmount 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.MinimumChargeAmount> {
                    pointService.charge(userId, invalidAmount)
                }
                exception.message shouldContain "최소 충전 금액"
                exception.message shouldContain "1,000원"
            }
        }
    }

    // ----- 1.2 최대 충전 금액 제한 (1,000,000원 이하) -----

    Given("[정책 1.2] 1,000,001원을 충전하려고 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val invalidAmount = 1000001L

        When("충전을 시도하면") {
            Then("MaximumChargeAmount 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.MaximumChargeAmount> {
                    pointService.charge(userId, invalidAmount)
                }
                exception.message shouldContain "최대 충전 금액"
                exception.message shouldContain "1,000,000원"
            }
        }
    }

    // ----- 1.3 충전 단위 제한 (100원 단위) -----

    Given("[정책 1.3] 1,050원을 충전하려고 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val invalidAmount = 1050L

        When("충전을 시도하면") {
            Then("InvalidChargeUnit 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.InvalidChargeUnit> {
                    pointService.charge(userId, invalidAmount)
                }
                exception.message shouldContain "100원 단위"
            }
        }
    }

    // ----- 검증 실패 시 DB 접근 안 함 검증 -----

    Given("[정책 검증] 여러 충전 검증 실패 케이스가 주어졌을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L

        When("각 검증을 실패시키면") {
            shouldThrow<PointException.MinimumChargeAmount> {
                pointService.charge(userId, 500L)
            }

            shouldThrow<PointException.MaximumChargeAmount> {
                pointService.charge(userId, 2000000L)
            }

            shouldThrow<PointException.InvalidChargeUnit> {
                pointService.charge(userId, 1150L)
            }

            Then("DB 접근이 일어나지 않아야 한다") {
                verify(exactly = 0) { userPointTable.selectById(any()) }
                verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { pointHistoryTable.insert(any(), any(), any(), any()) }
            }
        }
    }

    // ----- 충전 내역 저장 검증 -----

    Given("[기본 기능] 충전이 성공하는 경우") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val chargeAmount = 10000L

        every { userPointTable.selectById(userId) } returns UserPoint(userId, 5000L, System.currentTimeMillis())
        every { userPointTable.insertOrUpdate(any(), any()) } returns UserPoint(userId, 15000L, System.currentTimeMillis())
        every { pointHistoryTable.insert(any(), any(), any(), any()) } returns mockk(relaxed = true)

        When("포인트를 충전하면") {
            pointService.charge(userId, chargeAmount)

            Then("충전 내역이 저장되어야 한다") {
                verify(exactly = 1) {
                    pointHistoryTable.insert(
                        id = userId,
                        amount = chargeAmount,
                        transactionType = TransactionType.CHARGE,
                        updateMillis = any()
                    )
                }
            }
        }
    }

    // =============================================================================
    // 2. 포인트 사용 정책
    // =============================================================================

    // ----- 기본 사용 성공 시나리오 -----

    Given("[기본 기능] 사용자가 10,000원 보유하고 3,000원을 사용하려 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val currentPoint = 10000L
        val useAmount = 3000L
        val expectedPoint = currentPoint - useAmount

        every { userPointTable.selectById(userId) } returns UserPoint(userId, currentPoint, System.currentTimeMillis())
        every { pointHistoryTable.selectAllByUserId(userId) } returns emptyList()
        every { userPointTable.insertOrUpdate(userId, expectedPoint) } returns UserPoint(userId, expectedPoint, System.currentTimeMillis())
        every { pointHistoryTable.insert(any(), any(), any(), any()) } returns mockk(relaxed = true)

        When("포인트를 사용하면") {
            val result = pointService.use(userId, useAmount)

            Then("포인트는 7,000원이 되어야 한다") {
                result.point shouldBe expectedPoint
                verify(exactly = 1) { userPointTable.selectById(userId) }
                verify(exactly = 1) { userPointTable.insertOrUpdate(userId, expectedPoint) }
            }
        }
    }

    // ----- 2.1 잔고 부족 검증 -----

    Given("[정책 2.1] 잔고가 5,000원인데 10,000원을 사용하려 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val currentPoint = 5000L
        val useAmount = 10000L

        every { userPointTable.selectById(userId) } returns UserPoint(userId, currentPoint, System.currentTimeMillis())

        When("사용을 시도하면") {
            Then("InsufficientBalance 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.InsufficientBalance> {
                    pointService.use(userId, useAmount)
                }
                exception.message shouldContain "잔고가 부족합니다"

                verify(exactly = 1) { userPointTable.selectById(userId) }
                verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { pointHistoryTable.insert(any(), any(), any(), any()) }
            }
        }
    }

    // ----- 2.4 최소 사용 금액 제한 (100원 이상) -----

    Given("[정책 2.4] 50원을 사용하려 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val invalidAmount = 50L

        When("사용을 시도하면") {
            Then("MinimumUseAmount 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.MinimumUseAmount> {
                    pointService.use(userId, invalidAmount)
                }
                exception.message shouldContain "최소 사용 금액은 100원임"
                verify(exactly = 0) { userPointTable.selectById(any()) }
            }
        }
    }

    // ----- 2.2 사용 단위 제한 (100원 단위) -----

    Given("[정책 2.2] 1,050원을 사용하려 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val invalidAmount = 1050L

        When("사용을 시도하면") {
            Then("InvalidUseUnit 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.InvalidUseUnit> {
                    pointService.use(userId, invalidAmount)
                }
                exception.message shouldContain "포인트 사용은 100원 단위로만 가능합니다"
                verify(exactly = 0) { userPointTable.selectById(any()) }
            }
        }
    }

    // ----- 2.3 일일 사용 한도 제한 (100,000원) -----

    Given("[정책 2.3] 오늘 이미 90,000원을 사용한 상태에서 20,000원을 추가 사용하려 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val currentPoint = 100000L
        val useAmount = 20000L

        val todayHistories = listOf(
            PointHistory(1L, userId, TransactionType.USE, 50000L, System.currentTimeMillis()),
            PointHistory(2L, userId, TransactionType.USE, 40000L, System.currentTimeMillis())
        )

        every { userPointTable.selectById(userId) } returns UserPoint(userId, currentPoint, System.currentTimeMillis())
        every { pointHistoryTable.selectAllByUserId(userId) } returns todayHistories

        When("사용을 시도하면") {
            Then("DailyUseLimitExceeded 예외가 발생해야 한다") {
                val exception = shouldThrow<PointException.DailyUseLimitExceeded> {
                    pointService.use(userId, useAmount)
                }
                exception.message shouldContain "일일 사용 한도"
                exception.message shouldContain "초과했음"
                verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
                verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
            }
        }
    }

    Given("[정책 2.3] 오늘 50,000원을 사용한 상태에서 30,000원을 추가 사용하려 할 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val currentPoint = 100000L
        val useAmount = 30000L

        val todayHistories = listOf(
            PointHistory(1L, userId, TransactionType.USE, 50000L, System.currentTimeMillis())
        )

        every { userPointTable.selectById(userId) } returns UserPoint(userId, currentPoint, System.currentTimeMillis())
        every { pointHistoryTable.selectAllByUserId(userId) } returns todayHistories
        every { userPointTable.insertOrUpdate(userId, currentPoint - useAmount) } returns
            UserPoint(userId, currentPoint - useAmount, System.currentTimeMillis())
        every { pointHistoryTable.insert(any(), any(), any(), any()) } returns mockk(relaxed = true)

        When("사용하면") {
            val result = pointService.use(userId, useAmount)

            Then("성공적으로 사용되어야 한다") {
                result.point shouldBe (currentPoint - useAmount)
                verify(exactly = 1) { userPointTable.insertOrUpdate(userId, currentPoint - useAmount) }
            }
        }
    }

    // =============================================================================
    // 4. 포인트 내역 조회 정책
    // =============================================================================

    // ----- 4.2 사용자별 내역 격리 -----

    Given("[정책 4.2] 사용자에게 3건의 거래 내역이 있을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val histories = listOf(
            PointHistory(1L, userId, TransactionType.CHARGE, 10000L, System.currentTimeMillis()),
            PointHistory(2L, userId, TransactionType.USE, 3000L, System.currentTimeMillis()),
            PointHistory(3L, userId, TransactionType.CHARGE, 5000L, System.currentTimeMillis())
        )

        every { pointHistoryTable.selectAllByUserId(userId) } returns histories

        When("내역을 조회하면") {
            val result = pointService.getHistories(userId)

            Then("3건의 내역이 반환되어야 한다") {
                result shouldHaveSize 3
                result shouldBe histories
                verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
            }
        }
    }

    Given("[정책 4.2] 거래 내역이 없는 사용자가 주어졌을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 999L
        every { pointHistoryTable.selectAllByUserId(userId) } returns emptyList()

        When("내역을 조회하면") {
            val result = pointService.getHistories(userId)

            Then("빈 목록이 반환되어야 한다") {
                result.shouldBeEmpty()
                verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
            }
        }
    }

    // ----- 4.1 내역 제한 (최대 100건) -----

    Given("[정책 4.1] 150개의 거래 내역이 있는 사용자가 주어졌을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val manyHistories = (1..150).map { i ->
            PointHistory(
                i.toLong(),
                userId,
                if (i % 2 == 0) TransactionType.CHARGE else TransactionType.USE,
                1000L,
                System.currentTimeMillis() - (150 - i) * 1000
            )
        }

        every { pointHistoryTable.selectAllByUserId(userId) } returns manyHistories

        When("내역을 조회하면") {
            val result = pointService.getHistories(userId)

            Then("최대 100건만 반환되어야 한다") {
                result shouldHaveSize 100
                verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
            }
        }
    }

    // ----- 4.1 시간 역순 정렬 (최신순) -----

    Given("[정책 4.1] 시간순으로 정렬되지 않은 내역이 주어졌을 때") {
        val userPointTable = mockk<UserPointTable>()
        val pointHistoryTable = mockk<PointHistoryTable>()
        val pointService = PointService(userPointTable, pointHistoryTable)

        val userId = 1L
        val now = System.currentTimeMillis()
        val unsortedHistories = listOf(
            PointHistory(1L, userId, TransactionType.CHARGE, 1000L, now - 10000),
            PointHistory(2L, userId, TransactionType.USE, 500L, now),
            PointHistory(3L, userId, TransactionType.CHARGE, 2000L, now - 5000)
        )

        every { pointHistoryTable.selectAllByUserId(userId) } returns unsortedHistories

        When("내역을 조회하면") {
            val result = pointService.getHistories(userId)

            Then("시간 역순(최신순)으로 정렬되어야 한다") {
                result shouldHaveSize 3
                result[0].timeMillis shouldBeGreaterThanOrEqual result[1].timeMillis
                result[1].timeMillis shouldBeGreaterThanOrEqual result[2].timeMillis
            }
        }
    }
})
