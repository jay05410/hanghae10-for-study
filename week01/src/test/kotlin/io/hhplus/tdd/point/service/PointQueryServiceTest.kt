package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * PointQueryService 단위 테스트
 *
 * 책임: 조회 기능에 대한 Table 호출 검증
 */
class PointQueryServiceTest : DescribeSpec({
    val mockUserPointTable = mockk<UserPointTable>()
    val mockHistoryTable = mockk<PointHistoryTable>()
    val sut = PointQueryService(mockUserPointTable, mockHistoryTable)

    beforeEach {
        clearMocks(mockUserPointTable, mockHistoryTable)
    }

    describe("getPoint") {
        context("userId가 주어졌을 때") {
            it("UserPointTable에 조회를 지시하고 결과를 반환") {
                every { mockUserPointTable.selectById(1L) } returns UserPoint(1L, 10000L, 0L)

                val result = sut.getPoint(1L)

                result.id shouldBe 1L
                result.point shouldBe 10000L
                verify(exactly = 1) { mockUserPointTable.selectById(1L) }
            }
        }
    }

    describe("getHistories") {
        context("3건의 거래 내역이 있을 때") {
            it("PointHistoryTable에 조회를 지시하고 최신순으로 정렬하여 반환") {
                val histories = listOf(
                    PointHistory(1L, 1L, TransactionType.CHARGE, 10000L, 1000L),
                    PointHistory(2L, 1L, TransactionType.USE, 3000L, 3000L),
                    PointHistory(3L, 1L, TransactionType.CHARGE, 5000L, 2000L)
                )
                every { mockHistoryTable.selectAllByUserId(1L) } returns histories

                val result = sut.getHistories(1L)

                result shouldHaveSize 3
                result[0].id shouldBe 2L  // timeMillis 3000 (최신)
                result[1].id shouldBe 3L  // timeMillis 2000
                result[2].id shouldBe 1L  // timeMillis 1000 (오래됨)
                verify(exactly = 1) { mockHistoryTable.selectAllByUserId(1L) }
            }
        }

        context("150건의 거래 내역이 있을 때") {
            it("최대 100건만 반환") {
                val histories = (1..150).map { i ->
                    PointHistory(i.toLong(), 1L, TransactionType.CHARGE, 1000L, i.toLong() * 1000)
                }
                every { mockHistoryTable.selectAllByUserId(1L) } returns histories

                val result = sut.getHistories(1L)

                result shouldHaveSize 100
                verify(exactly = 1) { mockHistoryTable.selectAllByUserId(1L) }
            }
        }
    }
})
