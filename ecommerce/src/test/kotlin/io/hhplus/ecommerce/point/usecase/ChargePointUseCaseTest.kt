package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ChargePointUseCase 단위 테스트
 *
 * 책임: 포인트 충전 비즈니스 흐름 검증
 * - 다양한 서비스들의 조합을 통한 포인트 충전 프로세스 검증
 * - 트랜잭션 경계 내에서의 비즈니스 로직 순서 검증
 * - 신규 사용자 포인트 생성 로직 검증
 *
 * 검증 목표:
 * 1. 포인트 충전 전 사용자 포인트 존재 여부 확인 로직이 올바른가?
 * 2. 신규 사용자의 경우 포인트 계정 생성이 수행되는가?
 * 3. 충전 전후 잔액이 올바르게 기록되는가?
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
        context("기존 사용자의 포인트 충전") {
            it("기존 잔액을 조회하고 충전한 뒤 히스토리를 기록") {
                val userId = 1L
                val amount = 5000L
                val description = "테스트 충전"
                val balanceBefore = 10000L
                val balanceAfter = 15000L
                val pointAmount = PointAmount.of(amount)

                val existingUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                // 기존 포인트 조회
                every { mockPointService.getUserPoint(userId) } returns existingUserPoint
                // 충전 실행
                every { mockPointService.chargePoint(userId, pointAmount, userId, description) } returns updatedUserPoint
                // 히스토리 기록
                every { mockPointHistoryService.recordChargeHistory(userId, pointAmount, balanceBefore, balanceAfter, description) } returns mockk()

                val result = sut.execute(userId, amount, description)

                result shouldBe updatedUserPoint
                verifyOrder {
                    mockPointService.getUserPoint(userId)
                    mockPointService.chargePoint(userId, pointAmount, userId, description)
                    mockPointHistoryService.recordChargeHistory(userId, pointAmount, balanceBefore, balanceAfter, description)
                }
                verify(exactly = 0) { mockPointService.createUserPoint(any(), any()) }
            }
        }

        context("신규 사용자의 포인트 충전") {
            it("포인트 계정을 생성한 뒤 충전하고 히스토리를 기록") {
                val userId = 1L
                val amount = 5000L
                val description = "첫 충전"
                val balanceBefore = 0L
                val balanceAfter = 5000L
                val pointAmount = PointAmount.of(amount)

                val newUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val chargedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                // 신규 사용자 - 포인트 없음
                every { mockPointService.getUserPoint(userId) } returnsMany listOf(null, newUserPoint)
                // 포인트 계정 생성
                every { mockPointService.createUserPoint(userId, userId) } returns newUserPoint
                // 충전 실행
                every { mockPointService.chargePoint(userId, pointAmount, userId, description) } returns chargedUserPoint
                // 히스토리 기록
                every { mockPointHistoryService.recordChargeHistory(userId, pointAmount, balanceBefore, balanceAfter, description) } returns mockk()

                val result = sut.execute(userId, amount, description)

                result shouldBe chargedUserPoint
                verifyOrder {
                    mockPointService.getUserPoint(userId) // 첫 번째 조회 - null
                    mockPointService.createUserPoint(userId, userId) // 포인트 계정 생성
                    mockPointService.getUserPoint(userId) // 두 번째 조회 - 생성된 포인트
                    mockPointService.chargePoint(userId, pointAmount, userId, description)
                    mockPointHistoryService.recordChargeHistory(userId, pointAmount, balanceBefore, balanceAfter, description)
                }
            }
        }

        context("description이 null인 경우") {
            it("description을 null로 전달하여 충전과 히스토리 기록을 수행") {
                val userId = 1L
                val amount = 3000L
                val balanceBefore = 5000L
                val balanceAfter = 8000L
                val pointAmount = PointAmount.of(amount)

                val existingUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceBefore
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns balanceAfter
                }

                every { mockPointService.getUserPoint(userId) } returns existingUserPoint
                every { mockPointService.chargePoint(userId, pointAmount, userId, null) } returns updatedUserPoint
                every { mockPointHistoryService.recordChargeHistory(userId, pointAmount, balanceBefore, balanceAfter, null) } returns mockk()

                val result = sut.execute(userId, amount)

                result shouldBe updatedUserPoint
                verify(exactly = 1) { mockPointService.chargePoint(userId, pointAmount, userId, null) }
                verify(exactly = 1) { mockPointHistoryService.recordChargeHistory(userId, pointAmount, balanceBefore, balanceAfter, null) }
            }
        }

        context("PointAmount 검증") {
            it("주어진 amount로 PointAmount를 생성하여 사용") {
                val userId = 1L
                val amount = 10000L

                val existingUserPoint = mockk<UserPoint> {
                    every { balance } returns 0L
                }
                val updatedUserPoint = mockk<UserPoint> {
                    every { balance } returns amount
                }

                every { mockPointService.getUserPoint(userId) } returns existingUserPoint
                every { mockPointService.chargePoint(any(), any(), any(), any()) } returns updatedUserPoint
                every { mockPointHistoryService.recordChargeHistory(any(), any(), any(), any(), any()) } returns mockk()

                sut.execute(userId, amount)

                verify(exactly = 1) { mockPointService.chargePoint(userId, PointAmount.of(amount), userId, null) }
                verify(exactly = 1) { mockPointHistoryService.recordChargeHistory(userId, PointAmount.of(amount), 0L, amount, null) }
            }
        }
    }
})