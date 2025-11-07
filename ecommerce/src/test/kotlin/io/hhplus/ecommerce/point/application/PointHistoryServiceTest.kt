package io.hhplus.ecommerce.point.application

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * PointHistoryService 단위 테스트
 *
 * 책임: 포인트 이력 관리 서비스의 핵심 기능 검증
 * - 충전/차감 이력 기록 기능의 Repository 호출 검증
 * - 이력 조회 기능의 Repository 호출 검증
 * - 도메인 객체 생성 메서드 호출 검증
 *
 * 검증 목표:
 * 1. 각 이력 기록 메서드가 적절한 도메인 생성 메서드를 호출하는가?
 * 2. Repository 저장 메서드가 올바르게 호출되는가?
 * 3. 이력 조회 시 Repository 조회 메서드가 올바르게 호출되는가?
 */
class PointHistoryServiceTest : DescribeSpec({
    val mockPointHistoryRepository = mockk<PointHistoryRepository>()
    val sut = PointHistoryService(mockPointHistoryRepository)

    beforeEach {
        clearMocks(mockPointHistoryRepository)
    }

    describe("recordChargeHistory") {
        context("충전 이력 기록 시") {
            it("PointHistory.createChargeHistory를 호출하고 Repository에 저장") {
                val userId = 1L
                val amount = PointAmount.of(5000L)
                val balanceBefore = 10000L
                val balanceAfter = 15000L
                val description = "테스트 충전"
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createChargeHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = description
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordChargeHistory(userId, amount, balanceBefore, balanceAfter, description)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createChargeHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = description
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }

        context("충전 이력 기록 시 description이 null인 경우") {
            it("PointHistory.createChargeHistory를 description null로 호출하고 저장") {
                val userId = 1L
                val amount = PointAmount.of(5000L)
                val balanceBefore = 10000L
                val balanceAfter = 15000L
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createChargeHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = null
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordChargeHistory(userId, amount, balanceBefore, balanceAfter)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createChargeHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = null
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }
    }

    describe("recordDeductHistory") {
        context("차감 이력 기록 시") {
            it("PointHistory.createDeductHistory를 호출하고 Repository에 저장") {
                val userId = 1L
                val amount = PointAmount.of(3000L)
                val balanceBefore = 10000L
                val balanceAfter = 7000L
                val description = "테스트 차감"
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createDeductHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = description
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordDeductHistory(userId, amount, balanceBefore, balanceAfter, description)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createDeductHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = description
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }

        context("차감 이력 기록 시 description이 null인 경우") {
            it("PointHistory.createDeductHistory를 description null로 호출하고 저장") {
                val userId = 1L
                val amount = PointAmount.of(3000L)
                val balanceBefore = 10000L
                val balanceAfter = 7000L
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createDeductHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = null
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordDeductHistory(userId, amount, balanceBefore, balanceAfter)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createDeductHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = balanceAfter,
                        description = null
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }
    }

    describe("getPointHistories") {
        context("사용자 포인트 이력 조회 시") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val userId = 1L
                val expectedHistories = listOf(
                    mockk<PointHistory>(),
                    mockk<PointHistory>(),
                    mockk<PointHistory>()
                )
                every { mockPointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId) } returns expectedHistories

                val result = sut.getPointHistories(userId)

                result shouldBe expectedHistories
                verify(exactly = 1) { mockPointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId) }
            }
        }

        context("이력이 없는 사용자 조회 시") {
            it("빈 리스트를 반환") {
                val userId = 999L
                val expectedHistories = emptyList<PointHistory>()
                every { mockPointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId) } returns expectedHistories

                val result = sut.getPointHistories(userId)

                result shouldBe expectedHistories
                verify(exactly = 1) { mockPointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId) }
            }
        }
    }
})