package io.hhplus.ecommerce.point.application

import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * PointService 단위 테스트
 *
 * 책임: 포인트 서비스의 핵심 비즈니스 로직 검증
 * - 포인트 충전, 차감, 조회 기능 검증
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
        every { balance } returns pointAmount
        every { isActive } returns true
        every { createdAt } returns LocalDateTime.now()
        every { updatedAt } returns LocalDateTime.now()
        every { charge(any(), any()) } returns pointAmount
        every { deduct(any(), any()) } returns pointAmount
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

    describe("chargePoint") {
        context("정상적인 포인트 충전") {
            it("락을 걸고 포인트를 충전하여 저장") {
                val userId = 1L
                val amount = PointAmount(5000L)
                val chargedBy = 1L
                val mockUserPoint = createMockUserPoint(userId = userId)

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns mockUserPoint
                every { mockUserPointRepository.save(mockUserPoint) } returns mockUserPoint

                val result = sut.chargePoint(userId, amount, chargedBy)

                result shouldBe mockUserPoint
                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 1) { mockUserPoint.charge(amount, chargedBy) }
                verify(exactly = 1) { mockUserPointRepository.save(mockUserPoint) }
            }
        }

        context("존재하지 않는 사용자의 포인트 충전") {
            it("IllegalArgumentException을 발생") {
                val userId = 999L
                val amount = PointAmount(5000L)
                val chargedBy = 1L

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns null

                shouldThrow<IllegalArgumentException> {
                    sut.chargePoint(userId, amount, chargedBy)
                }

                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 0) { mockUserPointRepository.save(any()) }
            }
        }
    }

    describe("deductPoint") {
        context("정상적인 포인트 차감") {
            it("락을 걸고 포인트를 차감하여 저장") {
                val userId = 1L
                val amount = PointAmount(3000L)
                val deductedBy = 1L
                val mockUserPoint = createMockUserPoint(userId = userId)

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns mockUserPoint
                every { mockUserPointRepository.save(mockUserPoint) } returns mockUserPoint

                val result = sut.deductPoint(userId, amount, deductedBy)

                result shouldBe mockUserPoint
                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 1) { mockUserPoint.deduct(amount, deductedBy) }
                verify(exactly = 1) { mockUserPointRepository.save(mockUserPoint) }
            }
        }

        context("존재하지 않는 사용자의 포인트 차감") {
            it("IllegalArgumentException을 발생") {
                val userId = 999L
                val amount = PointAmount(3000L)
                val deductedBy = 1L

                every { mockUserPointRepository.findByUserIdWithLock(userId) } returns null

                shouldThrow<IllegalArgumentException> {
                    sut.deductPoint(userId, amount, deductedBy)
                }

                verify(exactly = 1) { mockUserPointRepository.findByUserIdWithLock(userId) }
                verify(exactly = 0) { mockUserPointRepository.save(any()) }
            }
        }
    }
})