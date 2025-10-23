package io.hhplus.tdd.point.service

import io.hhplus.tdd.common.exception.point.PointException
import io.hhplus.tdd.common.util.DateTimeUtil
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * PointUseService 단위 테스트
 *
 * 책임: 사용 기능에 대한 각 컴포넌트 호출 검증
 */
class PointUseServiceTest : DescribeSpec({
    val mockUserPointTable = mockk<UserPointTable>()
    val mockHistoryTable = mockk<PointHistoryTable>(relaxed = true)
    val mockQueryService = mockk<PointQueryService>()
    val sut = PointUseService(mockUserPointTable, mockHistoryTable, mockQueryService)

    beforeEach {
        clearMocks(mockUserPointTable, mockHistoryTable, mockQueryService)
    }

    describe("use") {
        context("정상 케이스") {
            it("10,000원에서 3,000원 사용 시 7,000원 반환하고 각 컴포넌트에 지시") {
                val todayUseHistories = listOf(
                    PointHistory(1L, 1L, TransactionType.USE, 50000L, DateTimeUtil.getTodayStartMillis() + 1000)
                )
                every { mockUserPointTable.selectById(1L) } returns UserPoint(1L, 10000L, 0L)
                every { mockQueryService.getHistories(1L) } returns todayUseHistories
                every { mockUserPointTable.insertOrUpdate(1L, 7000L) } returns UserPoint(1L, 7000L, 0L)

                val result = sut.use(1L, 3000L)

                result.point shouldBe 7000L
                verify(exactly = 1) { mockUserPointTable.selectById(1L) }
                verify(exactly = 1) { mockQueryService.getHistories(1L) }
                verify(exactly = 1) { mockUserPointTable.insertOrUpdate(1L, 7000L) }
                verify(exactly = 1) { mockHistoryTable.insert(1L, 3000L, TransactionType.USE, any()) }
            }
        }

        context("비즈니스 검증 실패 케이스") {
            it("잔고 부족 - 5,000원에서 10,000원 사용 시 예외 발생하고 조회만 수행") {
                every { mockUserPointTable.selectById(1L) } returns UserPoint(1L, 5000L, 0L)

                shouldThrow<PointException.InsufficientBalance> {
                    sut.use(1L, 10000L)
                }

                verify(exactly = 1) { mockUserPointTable.selectById(1L) }
                verify(exactly = 0) { mockQueryService.getHistories(any()) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }

            it("일일 한도 초과 - 오늘 90,000원 사용 후 20,000원 추가 사용 시 예외 발생") {
                val todayUseHistories = listOf(
                    PointHistory(1L, 1L, TransactionType.USE, 90000L, DateTimeUtil.getTodayStartMillis() + 1000)
                )
                every { mockUserPointTable.selectById(1L) } returns UserPoint(1L, 100000L, 0L)
                every { mockQueryService.getHistories(1L) } returns todayUseHistories

                shouldThrow<PointException.DailyUseLimitExceeded> {
                    sut.use(1L, 20000L)
                }

                verify(exactly = 1) { mockUserPointTable.selectById(1L) }
                verify(exactly = 1) { mockQueryService.getHistories(1L) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }
        }

        context("입력값 검증 실패 케이스") {
            it("최소 사용 금액 미달(50원) 시 예외 발생하고 어떤 컴포넌트에도 접근 안 함") {
                shouldThrow<PointException.MinimumUseAmount> {
                    sut.use(1L, 50L)
                }

                verify(exactly = 0) { mockUserPointTable.selectById(any()) }
                verify(exactly = 0) { mockQueryService.getHistories(any()) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }

            it("사용 단위 불일치(1,050원) 시 예외 발생하고 어떤 컴포넌트에도 접근 안 함") {
                shouldThrow<PointException.InvalidUseUnit> {
                    sut.use(1L, 1050L)
                }

                verify(exactly = 0) { mockUserPointTable.selectById(any()) }
                verify(exactly = 0) { mockQueryService.getHistories(any()) }
                verify(exactly = 0) { mockUserPointTable.insertOrUpdate(any(), any()) }
                verify(exactly = 0) { mockHistoryTable.insert(any(), any(), any(), any()) }
            }
        }
    }
})
