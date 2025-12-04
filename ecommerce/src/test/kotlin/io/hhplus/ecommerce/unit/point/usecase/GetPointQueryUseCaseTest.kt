package io.hhplus.ecommerce.unit.point.usecase

import io.hhplus.ecommerce.point.application.usecase.GetPointQueryUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.vo.Balance

class GetPointQueryUseCaseTest : DescribeSpec({

    val pointDomainService = mockk<PointDomainService>()
    val getPointQueryUseCase = GetPointQueryUseCase(pointDomainService)

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

                every { pointDomainService.getUserPoint(userId) } returns mockUserPoint

                // When
                val result = getPointQueryUseCase.getUserPoint(userId)

                // Then
                result shouldBe mockUserPoint
                verify { pointDomainService.getUserPoint(userId) }
            }

            it("should throw exception when user point not exists") {
                // Given
                val userId = 999L

                every { pointDomainService.getUserPoint(userId) } returns null

                // When & Then
                shouldThrow<IllegalArgumentException> {
                    getPointQueryUseCase.getUserPoint(userId)
                }
                verify { pointDomainService.getUserPoint(userId) }
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

                every { pointDomainService.getPointHistories(userId) } returns mockHistories

                // When
                val result = getPointQueryUseCase.getPointHistories(userId)

                // Then
                result shouldBe mockHistories
                verify { pointDomainService.getPointHistories(userId) }
            }

            it("should return empty list when no histories") {
                // Given
                val userId = 2L
                val emptyHistories = emptyList<PointHistory>()

                every { pointDomainService.getPointHistories(userId) } returns emptyHistories

                // When
                val result = getPointQueryUseCase.getPointHistories(userId)

                // Then
                result shouldBe emptyHistories
                verify { pointDomainService.getPointHistories(userId) }
            }
        }

        context("서비스 호출 확인 시") {
            it("should delegate to appropriate services") {
                // Given
                val userId = 3L
                val mockUserPoint = mockk<UserPoint>()
                val mockHistories = listOf(mockk<PointHistory>())

                every { pointDomainService.getUserPoint(userId) } returns mockUserPoint
                every { pointDomainService.getPointHistories(userId) } returns mockHistories

                // When
                getPointQueryUseCase.getUserPoint(userId)
                getPointQueryUseCase.getPointHistories(userId)

                // Then
                verify(exactly = 1) { pointDomainService.getUserPoint(userId) }
                verify(exactly = 1) { pointDomainService.getPointHistories(userId) }
            }
        }
    }
})