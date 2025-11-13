package io.hhplus.ecommerce.unit.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.usecase.ChargePointUseCase
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ChargePointUseCase 단위 테스트
 *
 * 책임: 포인트 적립 비즈니스 흐름 검증
 * - 다양한 서비스들의 조합을 통한 포인트 적립 프로세스 검증
 * - 트랜잭션 경계 내에서의 비즈니스 로직 순서 검증
 * - 신규 사용자 포인트 생성 로직 검증
 *
 * 검증 목표:
 * 1. 포인트 적립 전 사용자 포인트 존재 여부 확인 로직이 올바른가?
 * 2. 신규 사용자의 경우 포인트 계정 생성이 수행되는가?
 * 3. 적립 전후 잔액이 올바르게 기록되는가?
 * 4. 히스토리 기록이 적절한 정보로 수행되는가?
 * 5. 각 서비스 호출 순서가 올바른가?
 */
class ChargePointUseCaseTest : DescribeSpec({
    val mockPointService = mockk<PointService>()
    val mockPointHistoryService = mockk<PointHistoryService>()
    val sut = ChargePointUseCase(mockPointService, mockPointHistoryService)

    beforeEach {
        clearMocks(mockPointService, mockPointHistoryService)
    }

    describe("execute") {
        context("기존 사용자의 포인트 적립") {
            it("기존 잔액을 조회하고 적립한 뒤 히스토리를 기록") {
                val userId = 1L
                val amount = 5000L
                val description = "테스트 적립"
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(15000L)
                val pointAmount = PointAmount.of(amount)

                val existingUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                // 기존 포인트 조회
                every { mockPointService.getUserPoint(userId) } returns existingUserPoint
                // 적립 실행
                every { mockPointService.earnPoint(userId, pointAmount, userId, description) } returns updatedUserPoint
                // 히스토리 기록
                every { mockPointHistoryService.recordEarnHistory(userId, pointAmount, balanceBefore, balanceAfter, userId, description, isNull()) } returns mockk()

                val result = sut.execute(userId, amount, description)

                result shouldBe updatedUserPoint
                verifyOrder {
                    mockPointService.getUserPoint(userId)
                    mockPointService.earnPoint(userId, pointAmount, userId, description)
                    mockPointHistoryService.recordEarnHistory(userId, pointAmount, balanceBefore, balanceAfter, userId, description, isNull())
                }
                verify(exactly = 0) { mockPointService.createUserPoint(any(), any()) }
            }
        }

        context("신규 사용자의 포인트 적립") {
            it("포인트 계정을 생성한 뒤 적립하고 히스토리를 기록") {
                val userId = 1L
                val amount = 5000L
                val description = "첫 적립"
                val balanceBefore = Balance.zero()
                val balanceAfter = Balance.of(5000L)
                val pointAmount = PointAmount.of(amount)

                val newUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val earnedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                // 신규 사용자 - 포인트 없음
                every { mockPointService.getUserPoint(userId) } returnsMany listOf(null, newUserPoint)
                // 포인트 계정 생성
                every { mockPointService.createUserPoint(userId, userId) } returns newUserPoint
                // 적립 실행
                every { mockPointService.earnPoint(userId, pointAmount, userId, description) } returns earnedUserPoint
                // 히스토리 기록
                every { mockPointHistoryService.recordEarnHistory(userId, pointAmount, balanceBefore, balanceAfter, userId, description, isNull()) } returns mockk()

                val result = sut.execute(userId, amount, description)

                result shouldBe earnedUserPoint
                verifyOrder {
                    mockPointService.getUserPoint(userId) // 첫 번째 조회 - null
                    mockPointService.createUserPoint(userId, userId) // 포인트 계정 생성
                    mockPointService.getUserPoint(userId) // 두 번째 조회 - 생성된 포인트
                    mockPointService.earnPoint(userId, pointAmount, userId, description)
                    mockPointHistoryService.recordEarnHistory(userId, pointAmount, balanceBefore, balanceAfter, userId, description, isNull())
                }
            }
        }

        context("description이 null인 경우") {
            it("description을 null로 전달하여 적립과 히스토리 기록을 수행") {
                val userId = 1L
                val amount = 3000L
                val balanceBefore = Balance.of(5000L)
                val balanceAfter = Balance.of(8000L)
                val pointAmount = PointAmount.of(amount)

                val existingUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                every { mockPointService.getUserPoint(userId) } returns existingUserPoint
                every { mockPointService.earnPoint(userId, pointAmount, userId, null) } returns updatedUserPoint
                every { mockPointHistoryService.recordEarnHistory(userId, pointAmount, balanceBefore, balanceAfter, userId, null, isNull()) } returns mockk()

                val result = sut.execute(userId, amount)

                result shouldBe updatedUserPoint
                verify(exactly = 1) { mockPointService.earnPoint(userId, pointAmount, userId, null) }
                verify(exactly = 1) { mockPointHistoryService.recordEarnHistory(userId, pointAmount, balanceBefore, balanceAfter, userId, null, isNull()) }
            }
        }

        context("PointAmount 검증") {
            it("주어진 amount로 PointAmount를 생성하여 사용") {
                val userId = 1L
                val amount = 10000L

                val existingUserPoint = mockk<UserPoint> {
                    every { balance } returns Balance.zero()
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns Balance.of(amount)
                }

                every { mockPointService.getUserPoint(userId) } returns existingUserPoint
                every { mockPointService.earnPoint(userId, PointAmount.of(amount), userId, null) } returns updatedUserPoint
                every { mockPointHistoryService.recordEarnHistory(userId, PointAmount.of(amount), Balance.zero(), Balance.of(amount), userId, null, isNull()) } returns mockk()

                sut.execute(userId, amount)

                verify(exactly = 1) { mockPointService.earnPoint(userId, PointAmount.of(amount), userId, null) }
                verify(exactly = 1) { mockPointHistoryService.recordEarnHistory(userId, PointAmount.of(amount), Balance.zero(), Balance.of(amount), userId, null, isNull()) }
            }
        }
    }
})