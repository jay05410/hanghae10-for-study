package io.hhplus.tdd.point.service

import io.hhplus.tdd.common.exception.point.PointException
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * PointChargeService 단위 테스트
 *
 * 책임: 충전 기능에 대한 Table 호출 검증
 */
class PointChargeServiceTest : DescribeSpec({
    val mockUserPointTable = mockk<UserPointTable>()
    val mockHistoryTable = mockk<PointHistoryTable>(relaxed = true)
    val sut = PointChargeService(mockUserPointTable, mockHistoryTable)

    beforeEach {
        clearMocks(mockUserPointTable, mockHistoryTable)
    }

    describe("charge") {
        context("정상 케이스") {
            it("10,000원에서 5,000원 충전 시 15,000원 반환하고 각 컴포넌트에 지시") {
                every { mockUserPointTable.selectById(1L) } returns UserPoint(1L, 10000L, 0L)
                every { mockUserPointTable.insertOrUpdate(1L, 15000L) } returns UserPoint(1L, 15000L, 0L)

                val result = sut.charge(1L, 5000L)

                result.point shouldBe 15000L
                verify(exactly = 1) { mockUserPointTable.selectById(1L) }
                verify(exactly = 1) { mockUserPointTable.insertOrUpdate(1L, 15000L) }
                verify(exactly = 1) { mockHistoryTable.insert(1L, 5000L, TransactionType.CHARGE, any()) }
            }
        }

        context("검증 실패 케이스") {
            it("최소 충전 금액 미달(999원) 시 예외 발생하고 DB 접근 없음") {
                shouldThrow<PointException.MinimumChargeAmount> {
                    sut.charge(1L, 999L)
                }

                verify(exactly = 0) { mockUserPointTable.selectById(any()) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }

            it("최대 충전 금액 초과(1,000,001원) 시 예외 발생하고 DB 접근 없음") {
                shouldThrow<PointException.MaximumChargeAmount> {
                    sut.charge(1L, 1000001L)
                }

                verify(exactly = 0) { mockUserPointTable.selectById(any()) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }

            it("충전 단위 불일치(1,050원) 시 예외 발생하고 DB 접근 없음") {
                shouldThrow<PointException.InvalidChargeUnit> {
                    sut.charge(1L, 1050L)
                }

                verify(exactly = 0) { mockUserPointTable.selectById(any()) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }
        }
    }
})
