package io.hhplus.ecommerce.unit.coupon.application

import io.hhplus.ecommerce.coupon.application.CouponIssueHistoryService
import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.repository.CouponIssueHistoryRepository
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * CouponIssueHistoryService 단위 테스트
 *
 * 책임: 쿠폰 발급 이력 관리 서비스의 핵심 기능 검증
 * - 쿠폰 발급, 사용, 만료 이력 기록 기능의 Repository 호출 검증
 * - 이력 조회 및 통계 계산 기능 검증
 * - 도메인 객체 생성 메서드 호출 검증
 *
 * 검증 목표:
 * 1. 각 이력 기록 메서드가 적절한 도메인 생성 메서드를 호출하는가?
 * 2. Repository 저장 메서드가 올바르게 호출되는가?
 * 3. 이력 조회 시 Repository 조회 메서드가 올바르게 호출되는가?
 * 4. 통계 계산이 올바르게 수행되는가?
 */
class CouponIssueHistoryServiceTest : DescribeSpec({
    val mockCouponIssueHistoryRepository = mockk<CouponIssueHistoryRepository>()
    val sut = CouponIssueHistoryService(mockCouponIssueHistoryRepository)

    beforeEach {
        clearMocks(mockCouponIssueHistoryRepository)
    }

    describe("recordIssue") {
        context("쿠폰 발급 이력 기록") {
            it("CouponIssueHistory.createIssueHistory를 호출하고 Repository에 저장") {
                val couponId = 1L
                val userId = 1L
                val couponName = "할인쿠폰"
                val mockHistory = mockk<CouponIssueHistory>()

                mockkObject(CouponIssueHistory.Companion)
                every {
                    CouponIssueHistory.createIssueHistory(
                        couponId = couponId,
                        userId = userId,
                        issuedAt = any(),
                        description = "쿠폰 발급: $couponName"
                    )
                } returns mockHistory
                every { mockCouponIssueHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordIssue(couponId, userId, couponName)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    CouponIssueHistory.createIssueHistory(
                        couponId = couponId,
                        userId = userId,
                        issuedAt = any(),
                        description = "쿠폰 발급: $couponName"
                    )
                }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.save(mockHistory) }
            }
        }
    }

    describe("recordUsage") {
        context("쿠폰 사용 이력 기록") {
            it("CouponIssueHistory.createUsageHistory를 호출하고 Repository에 저장") {
                val couponId = 1L
                val userId = 1L
                val couponName = "할인쿠폰"
                val orderId = 1L
                val issuedAt = LocalDateTime.now().minusHours(1)
                val mockHistory = mockk<CouponIssueHistory>()

                mockkObject(CouponIssueHistory.Companion)
                every {
                    CouponIssueHistory.createUsageHistory(
                        couponId = couponId,
                        userId = userId,
                        issuedAt = issuedAt,
                        usedAt = any(),
                        description = "쿠폰 사용: $couponName, 주문 ID: $orderId"
                    )
                } returns mockHistory
                every { mockCouponIssueHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordUsage(couponId, userId, couponName, orderId, issuedAt)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    CouponIssueHistory.createUsageHistory(
                        couponId = couponId,
                        userId = userId,
                        issuedAt = issuedAt,
                        usedAt = any(),
                        description = "쿠폰 사용: $couponName, 주문 ID: $orderId"
                    )
                }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.save(mockHistory) }
            }
        }
    }

    describe("recordExpiration") {
        context("쿠폰 만료 이력 기록") {
            it("CouponIssueHistory.createExpirationHistory를 호출하고 Repository에 저장") {
                val couponId = 1L
                val userId = 1L
                val couponName = "만료쿠폰"
                val issuedAt = LocalDateTime.now().minusDays(30)
                val mockHistory = mockk<CouponIssueHistory>()

                mockkObject(CouponIssueHistory.Companion)
                every {
                    CouponIssueHistory.createExpirationHistory(
                        couponId = couponId,
                        userId = userId,
                        issuedAt = issuedAt,
                        expiredAt = any(),
                        description = "쿠폰 만료: $couponName"
                    )
                } returns mockHistory
                every { mockCouponIssueHistoryRepository.save(mockHistory) } returns mockHistory

                val result = sut.recordExpiration(couponId, userId, couponName, issuedAt)

                result shouldBe mockHistory
                verify(exactly = 1) {
                    CouponIssueHistory.createExpirationHistory(
                        couponId = couponId,
                        userId = userId,
                        issuedAt = issuedAt,
                        expiredAt = any(),
                        description = "쿠폰 만료: $couponName"
                    )
                }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.save(mockHistory) }
            }
        }
    }

    describe("getUserCouponHistory") {
        context("사용자 쿠폰 이력 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val userId = 1L
                val expectedHistories = listOf(
                    mockk<CouponIssueHistory>(),
                    mockk<CouponIssueHistory>()
                )

                every { mockCouponIssueHistoryRepository.findByUserId(userId) } returns expectedHistories

                val result = sut.getUserCouponHistory(userId)

                result shouldBe expectedHistories
                verify(exactly = 1) { mockCouponIssueHistoryRepository.findByUserId(userId) }
            }
        }

        context("이력이 없는 사용자 조회") {
            it("빈 리스트를 반환") {
                val userId = 999L
                val emptyHistories = emptyList<CouponIssueHistory>()

                every { mockCouponIssueHistoryRepository.findByUserId(userId) } returns emptyHistories

                val result = sut.getUserCouponHistory(userId)

                result shouldBe emptyHistories
                verify(exactly = 1) { mockCouponIssueHistoryRepository.findByUserId(userId) }
            }
        }
    }

    describe("getCouponHistory") {
        context("쿠폰별 이력 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val couponId = 1L
                val expectedHistories = listOf(
                    mockk<CouponIssueHistory>(),
                    mockk<CouponIssueHistory>(),
                    mockk<CouponIssueHistory>()
                )

                every { mockCouponIssueHistoryRepository.findByCouponId(couponId) } returns expectedHistories

                val result = sut.getCouponHistory(couponId)

                result shouldBe expectedHistories
                verify(exactly = 1) { mockCouponIssueHistoryRepository.findByCouponId(couponId) }
            }
        }
    }

    describe("getCouponStatistics") {
        context("쿠폰 통계 조회") {
            it("각 상태별 개수를 조회하고 통계를 계산하여 반환") {
                val couponId = 1L
                val issuedCount = 100L
                val usedCount = 80L
                val expiredCount = 5L
                val expectedUsageRate = 80.0

                every { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.ISSUED) } returns issuedCount
                every { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.USED) } returns usedCount
                every { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.EXPIRED) } returns expiredCount

                val result = sut.getCouponStatistics(couponId)

                result.couponId shouldBe couponId
                result.totalIssued shouldBe issuedCount
                result.totalUsed shouldBe usedCount
                result.totalExpired shouldBe expiredCount
                result.usageRate shouldBe expectedUsageRate

                verify(exactly = 1) { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.ISSUED) }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.USED) }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.EXPIRED) }
            }
        }

        context("발급된 쿠폰이 없는 경우") {
            it("사용률을 0으로 계산") {
                val couponId = 999L
                val issuedCount = 0L
                val usedCount = 0L
                val expiredCount = 0L

                every { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.ISSUED) } returns issuedCount
                every { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.USED) } returns usedCount
                every { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.EXPIRED) } returns expiredCount

                val result = sut.getCouponStatistics(couponId)

                result.usageRate shouldBe 0.0
                verify(exactly = 1) { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.ISSUED) }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.USED) }
                verify(exactly = 1) { mockCouponIssueHistoryRepository.countByCouponIdAndStatus(couponId, UserCouponStatus.EXPIRED) }
            }
        }
    }

    describe("getRecentIssueHistory") {
        context("최근 발급 이력 조회 (기본값)") {
            it("Repository에서 ISSUED 상태 이력을 조회하고 최신순으로 정렬하여 100건만 반환") {
                val now = LocalDateTime.now()
                val histories = listOf(
                    mockk<CouponIssueHistory> { every { issuedAt } returns now },
                    mockk<CouponIssueHistory> { every { issuedAt } returns now.minusHours(1) },
                    mockk<CouponIssueHistory> { every { issuedAt } returns now.minusHours(2) }
                )

                every { mockCouponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED) } returns histories

                val result = sut.getRecentIssueHistory()

                result.size shouldBe 3 // 실제로는 정렬된 상위 100개만 반환되지만 테스트에서는 3개
                verify(exactly = 1) { mockCouponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED) }
            }
        }

        context("최근 발급 이력 조회 (사용자 지정 limit)") {
            it("지정된 개수만큼만 반환") {
                val limit = 50
                val histories = (1..100).map { mockk<CouponIssueHistory> { every { issuedAt } returns LocalDateTime.now() } }

                every { mockCouponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED) } returns histories

                val result = sut.getRecentIssueHistory(limit)

                result.size shouldBe limit
                verify(exactly = 1) { mockCouponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED) }
            }
        }

        context("발급 이력이 없는 경우") {
            it("빈 리스트를 반환") {
                val emptyHistories = emptyList<CouponIssueHistory>()

                every { mockCouponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED) } returns emptyHistories

                val result = sut.getRecentIssueHistory()

                result shouldBe emptyList()
                verify(exactly = 1) { mockCouponIssueHistoryRepository.findByStatus(UserCouponStatus.ISSUED) }
            }
        }
    }
})