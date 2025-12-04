package io.hhplus.ecommerce.unit.point.application

import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * PointDomainService 단위 테스트 (이력 기록 기능)
 *
 * 책임: 포인트 이력 관리 기능의 핵심 기능 검증
 * - 적립/사용 이력 기록 기능의 Repository 호출 검증
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
    val mockUserPointRepository = mockk<UserPointRepository>()
    val sut = PointDomainService(mockUserPointRepository, mockPointHistoryRepository)

    beforeEach {
        clearMocks(mockPointHistoryRepository)
    }

    describe("recordEarnHistory") {
        context("적립 이력 기록 시") {
            it("PointHistory.createEarnHistory를 호출하고 Repository에 저장") {
                val userId = 1L
                val amount = PointAmount.of(5000L)
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(15000L)
                val description = "테스트 적립"
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createEarnHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = description
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordEarnHistory(userId, amount, balanceBefore, balanceAfter, description)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createEarnHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = description
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }

        context("적립 이력 기록 시 description이 null인 경우") {
            it("PointHistory.createEarnHistory를 description null로 호출하고 저장") {
                val userId = 1L
                val amount = PointAmount.of(5000L)
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(15000L)
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createEarnHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = null
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordEarnHistory(userId, amount, balanceBefore, balanceAfter)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createEarnHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = null
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }
    }

    describe("recordUseHistory") {
        context("사용 이력 기록 시") {
            it("PointHistory.createUseHistory를 호출하고 Repository에 저장") {
                val userId = 1L
                val amount = PointAmount.of(3000L)
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(7000L)
                val description = "테스트 사용"
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createUseHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = description
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordUseHistory(userId, amount, balanceBefore, balanceAfter, description)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createUseHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = description
                    )
                }
                verify(exactly = 1) { mockPointHistoryRepository.save(mockHistory) }
            }
        }

        context("사용 이력 기록 시 description이 null인 경우") {
            it("PointHistory.createUseHistory를 description null로 호출하고 저장") {
                val userId = 1L
                val amount = PointAmount.of(3000L)
                val balanceBefore = Balance.of(10000L)
                val balanceAfter = Balance.of(7000L)
                val mockHistory = mockk<PointHistory>()

                mockkObject(PointHistory.Companion)
                every {
                    PointHistory.createUseHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
                        description = null
                    )
                } returns mockHistory
                every { mockPointHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordUseHistory(userId, amount, balanceBefore, balanceAfter)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    PointHistory.createUseHistory(
                        userId = userId,
                        amount = amount,
                        balanceBefore = balanceBefore.value,
                        balanceAfter = balanceAfter.value,
                        orderId = null,
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
