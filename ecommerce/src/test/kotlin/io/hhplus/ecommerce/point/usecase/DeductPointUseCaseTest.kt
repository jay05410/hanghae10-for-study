package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * DeductPointUseCase 단위 테스트
 *
 * 책임: 포인트 사용 비즈니스 흐름 검증
 * - 포인트 사용 프로세스의 서비스 조합 검증
 * - 사용 전 사용자 포인트 존재 여부 검증
 * - 히스토리 기록 로직 검증
 *
 * 검증 목표:
 * 1. 포인트 사용 전 사용자 포인트 존재 여부 확인이 수행되는가?
 * 2. 사용자 포인트가 없는 경우 적절한 예외가 발생하는가?
 * 3. 사용 전후 잔액이 올바르게 기록되는가?
 * 4. 히스토리 기록이 적절한 정보로 수행되는가?
 * 5. 각 서비스 호출 순서가 올바른가?
 */
class DeductPointUseCaseTest : DescribeSpec({
    val mockPointService = mockk<PointService>()
    val mockPointHistoryService = mockk<PointHistoryService>()
    val sut = DeductPointUseCase(mockPointService, mockPointHistoryService)

    beforeEach {
        clearMocks(mockPointService, mockPointHistoryService)
    }

    describe("execute") {
        context("정상 포인트 사용") {
            it("기존 잔액을 조회하고 사용한 뒤 히스토리를 기록") {
                val userId = 1L
                val amount = 3000L
                val description = "테스트 사용"
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(7000L)
                val pointAmount = PointAmount.of(amount)

                val userPointBefore = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                // 사용 전 포인트 조회
                every { mockPointService.getUserPoint(userId) } returns userPointBefore
                // 사용 실행
                every { mockPointService.usePoint(userId, pointAmount, userId, description) } returns updatedUserPoint
                // 히스토리 기록
                every { mockPointHistoryService.recordUseHistory(userId, pointAmount, balanceBefore, balanceAfter, description) } returns mockk()

                val result = sut.execute(userId, amount, description)

                result shouldBe updatedUserPoint
                verifyOrder {
                    mockPointService.getUserPoint(userId)
                    mockPointService.usePoint(userId, pointAmount, userId, description)
                    mockPointHistoryService.recordUseHistory(userId, pointAmount, balanceBefore, balanceAfter, description)
                }
            }
        }

        context("사용자 포인트가 존재하지 않는 경우") {
            it("IllegalArgumentException을 발생시키고 사용 및 히스토리 기록을 수행하지 않음") {
                val userId = 999L
                val amount = 3000L
                val description = "테스트 사용"

                every { mockPointService.getUserPoint(userId) } returns null

                shouldThrow<IllegalArgumentException> {
                    sut.execute(userId, amount, description)
                }.message shouldBe "사용자 포인트 정보가 없습니다: $userId"

                verify(exactly = 1) { mockPointService.getUserPoint(userId) }
                verify(exactly = 0) { mockPointService.usePoint(any(), any(), any(), any()) }
                verify(exactly = 0) { mockPointHistoryService.recordUseHistory(any(), any(), any(), any(), any()) }
            }
        }

        context("description이 null인 경우") {
            it("description을 null로 전달하여 사용과 히스토리 기록을 수행") {
                val userId = 1L
                val amount = 2000L
                val balanceBefore = Balance.of(5000L)
                val balanceAfter = Balance.of(3000L)
                val pointAmount = PointAmount.of(amount)

                val userPointBefore = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                every { mockPointService.getUserPoint(userId) } returns userPointBefore
                every { mockPointService.usePoint(userId, pointAmount, userId, null) } returns updatedUserPoint
                every { mockPointHistoryService.recordUseHistory(userId, pointAmount, balanceBefore, balanceAfter, null) } returns mockk()

                val result = sut.execute(userId, amount)

                result shouldBe updatedUserPoint
                verify(exactly = 1) { mockPointService.usePoint(userId, pointAmount, userId, null) }
                verify(exactly = 1) { mockPointHistoryService.recordUseHistory(userId, pointAmount, balanceBefore, balanceAfter, null) }
            }
        }

        context("PointAmount 검증") {
            it("주어진 amount로 PointAmount를 생성하여 사용") {
                val userId = 1L
                val amount = 5000L
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(5000L)

                val userPointBefore = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                every { mockPointService.getUserPoint(userId) } returns userPointBefore
                every { mockPointService.usePoint(any(), any(), any(), any()) } returns updatedUserPoint
                every { mockPointHistoryService.recordUseHistory(any(), any(), any(), any(), any()) } returns mockk()

                sut.execute(userId, amount)

                verify(exactly = 1) { mockPointService.usePoint(userId, PointAmount.of(amount), userId, null) }
                verify(exactly = 1) { mockPointHistoryService.recordUseHistory(userId, PointAmount.of(amount), balanceBefore, balanceAfter, null) }
            }
        }

        context("잔액 변화 추적") {
            it("사용 전후의 정확한 잔액을 히스토리에 기록") {
                val userId = 1L
                val amount = 1500L
                val balanceBefore = Balance.of(8000L)
                val balanceAfter = Balance.of(6500L)
                val pointAmount = PointAmount.of(amount)

                val userPointBefore = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                every { mockPointService.getUserPoint(userId) } returns userPointBefore
                every { mockPointService.usePoint(userId, pointAmount, userId, null) } returns updatedUserPoint
                every { mockPointHistoryService.recordUseHistory(userId, pointAmount, balanceBefore, balanceAfter, null) } returns mockk()

                sut.execute(userId, amount)

                // 사용 전 잔액과 사용 후 잔액이 정확히 전달되는지 검증
                verify(exactly = 1) {
                    mockPointHistoryService.recordUseHistory(
                        userId = userId,
                        amount = pointAmount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = null
                    )
                }
            }
        }
    }
})