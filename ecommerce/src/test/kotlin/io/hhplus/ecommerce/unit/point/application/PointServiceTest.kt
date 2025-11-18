package io.hhplus.ecommerce.unit.point.application

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.exception.PointException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * PointService 단위 테스트
 *
 * 책임: 포인트 서비스의 핵심 비즈니스 로직 검증
 * - 포인트 적립, 사용, 조회 기능 검증
 * - Repository와의 상호작용 검증
 */
class PointServiceTest : DescribeSpec({
    val mockUserPointRepository = mockk<UserPointRepository>()
    val sut = PointService(mockUserPointRepository)

    fun createMockUserPoint(
        id: Long = 1L,
        userId: Long = 1L,
        pointAmount: Long = 10000L
    ): UserPoint = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.userId } returns userId
        every { balance } returns Balance.of(pointAmount)
        every { isDeleted() } returns false
        every { createdAt } returns LocalDateTime.now()
        every { updatedAt } returns LocalDateTime.now()
        every { earn(any(), any()) } returns Balance.of(pointAmount)
        every { use(any(), any()) } returns Balance.of(pointAmount)
    }

    beforeEach {
        clearMocks(mockUserPointRepository)
    }

    describe("getUserPoint") {
        context("존재하는 사용자 포인트 조회") {
            it("Repository에서 사용자 포인트를 조회하여 반환") {
                val userId = 1L
                val mockUserPoint = createMockUserPoint(userId = userId)

                every { mockUserPointRepository.findByUserId(userId) } returns mockUserPoint

                val result = sut.getUserPoint(userId)

                result shouldBe mockUserPoint
                verify(exactly = 1) { mockUserPointRepository.findByUserId(userId) }
            }
        }

        context("존재하지 않는 사용자 포인트 조회") {
            it("null을 반환") {
                val userId = 999L

                every { mockUserPointRepository.findByUserId(userId) } returns null

                val result = sut.getUserPoint(userId)

                result shouldBe null
                verify(exactly = 1) { mockUserPointRepository.findByUserId(userId) }
            }
        }
    }

    describe("createUserPoint") {
        context("새로운 사용자 포인트 생성") {
            it("포인트를 생성하고 저장하여 반환") {
                val userId = 1L
                val createdBy = 1L
                val mockUserPoint = createMockUserPoint(userId = userId)

                every { mockUserPointRepository.save(any()) } returns mockUserPoint

                val result = sut.createUserPoint(userId, createdBy)

                result shouldBe mockUserPoint
                verify(exactly = 1) { mockUserPointRepository.save(any()) }
            }
        }
    }

    describe("earnPoint") {
        context("정상적인 포인트 적립") {
            it("락을 걸고 포인트를 적립하여 저장") {
                val userId = 1L
                val amount = PointAmount(5000L)
                val chargedBy = 1L
                val mockUserPoint = createMockUserPoint(userId = userId)

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns mockUserPoint
                every { mockUserPointRepository.save(mockUserPoint) } returns mockUserPoint

                val result = sut.earnPoint(userId, amount, chargedBy)

                result shouldBe mockUserPoint
                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 1) { mockUserPoint.earn(amount, chargedBy) }
                verify(exactly = 1) { mockUserPointRepository.save(mockUserPoint) }
            }
        }

        context("존재하지 않는 사용자의 포인트 적립") {
            it("PointException.PointNotFound를 발생") {
                val userId = 999L
                val amount = PointAmount(5000L)
                val chargedBy = 1L

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns null

                shouldThrow<PointException.PointNotFound> {
                    sut.earnPoint(userId, amount, chargedBy)
                }

                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 0) { mockUserPointRepository.save(any()) }
            }
        }
    }

    describe("usePoint") {
        context("정상적인 포인트 사용") {
            it("락을 걸고 포인트를 사용하여 저장") {
                val userId = 1L
                val amount = PointAmount(3000L)
                val deductedBy = 1L
                val mockUserPoint = createMockUserPoint(userId = userId)

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns mockUserPoint
                every { mockUserPointRepository.save(mockUserPoint) } returns mockUserPoint

                val result = sut.usePoint(userId, amount, deductedBy)

                result shouldBe mockUserPoint
                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 1) { mockUserPoint.use(amount, deductedBy) }
                verify(exactly = 1) { mockUserPointRepository.save(mockUserPoint) }
            }
        }

        context("존재하지 않는 사용자의 포인트 사용") {
            it("PointException.PointNotFound를 발생") {
                val userId = 999L
                val amount = PointAmount(3000L)
                val deductedBy = 1L

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns null

                shouldThrow<PointException.PointNotFound> {
                    sut.usePoint(userId, amount, deductedBy)
                }

                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 0) { mockUserPointRepository.save(any()) }
            }
        }
    }
})