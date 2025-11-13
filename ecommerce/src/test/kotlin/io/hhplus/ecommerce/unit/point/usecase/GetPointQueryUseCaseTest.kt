package io.hhplus.ecommerce.unit.point.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.vo.Balance

class GetPointQueryUseCaseTest : DescribeSpec({

    val pointService = mockk<PointService>()
    val pointHistoryService = mockk<PointHistoryService>()
    val getPointQueryUseCase = GetPointQueryUseCase(pointService, pointHistoryService)

    describe("GetPointQueryUseCase") {

        beforeEach {
            clearAllMocks()
        }

        context("사용자 포인트 조회 시") {
            it("should return user point when exists") {
                // Given
                val userId = 1L
                val mockUserPoint = mockk<UserPoint> {
                    every { id } returns userId
                    every { balance } returns Balance.of(1000L)
                }

                every { pointService.getUserPoint(userId) } returns mockUserPoint

                // When
                val result = getPointQueryUseCase.getUserPoint(userId)

                // Then
                result shouldBe mockUserPoint
                verify { pointService.getUserPoint(userId) }
            }

            it("should throw exception when user point not exists") {
                // Given
                val userId = 999L

                every { pointService.getUserPoint(userId) } returns null

                // When & Then
                shouldThrow<IllegalArgumentException> {
                    getPointQueryUseCase.getUserPoint(userId)
                }
                verify { pointService.getUserPoint(userId) }
            }
        }

        context("포인트 거래 내역 조회 시") {
            it("should return point histories") {
                // Given
                val userId = 1L
                val mockHistories = listOf(
                    mockk<PointHistory> { every { id } returns 1L },
                    mockk<PointHistory> { every { id } returns 2L }
                )

                every { pointHistoryService.getPointHistories(userId) } returns mockHistories

                // When
                val result = getPointQueryUseCase.getPointHistories(userId)

                // Then
                result shouldBe mockHistories
                verify { pointHistoryService.getPointHistories(userId) }
            }

            it("should return empty list when no histories") {
                // Given
                val userId = 2L
                val emptyHistories = emptyList<PointHistory>()

                every { pointHistoryService.getPointHistories(userId) } returns emptyHistories

                // When
                val result = getPointQueryUseCase.getPointHistories(userId)

                // Then
                result shouldBe emptyHistories
                verify { pointHistoryService.getPointHistories(userId) }
            }
        }

        context("서비스 호출 확인 시") {
            it("should delegate to appropriate services") {
                // Given
                val userId = 3L
                val mockUserPoint = mockk<UserPoint>()
                val mockHistories = listOf(mockk<PointHistory>())

                every { pointService.getUserPoint(userId) } returns mockUserPoint
                every { pointHistoryService.getPointHistories(userId) } returns mockHistories

                // When
                getPointQueryUseCase.getUserPoint(userId)
                getPointQueryUseCase.getPointHistories(userId)

                // Then
                verify(exactly = 1) { pointService.getUserPoint(userId) }
                verify(exactly = 1) { pointHistoryService.getPointHistories(userId) }
            }
        }
    }
})