package io.hhplus.ecommerce.unit.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * GetUserReservationsUseCase 단위 테스트
 *
 * 책임: 사용자 재고 예약 조회 유스케이스의 동작 검증
 * - 사용자별 예약 내역 조회 기능 검증
 * - StockReservationService와의 상호작용 검증
 */
class GetUserReservationsUseCaseTest : DescribeSpec({
    val mockStockReservationService = mockk<StockReservationService>()
    val sut = GetUserReservationsUseCase(mockStockReservationService)

    fun createMockReservation(
        id: Long = 1L,
        productId: Long = 1L,
        userId: Long = 1L,
        quantity: Int = 5,
        status: ReservationStatus = ReservationStatus.RESERVED
    ): StockReservation = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.productId } returns productId
        every { this@mockk.userId } returns userId
        every { this@mockk.quantity } returns quantity
        every { this@mockk.status } returns status
        every { createdAt } returns LocalDateTime.now()
        every { expiresAt } returns LocalDateTime.now().plusMinutes(20)
    }

    beforeEach {
        clearMocks(mockStockReservationService)
    }

    describe("execute") {
        context("사용자의 예약 내역 조회") {
            it("서비스를 통해 사용자 예약 목록을 반환") {
                val userId = 1L
                val expectedReservations = listOf(
                    createMockReservation(id = 1L, userId = userId, productId = 1L),
                    createMockReservation(id = 2L, userId = userId, productId = 2L)
                )

                every { mockStockReservationService.getUserReservations(userId) } returns expectedReservations

                val result = sut.execute(userId)

                result shouldBe expectedReservations
                verify(exactly = 1) { mockStockReservationService.getUserReservations(userId) }
            }
        }

        context("예약이 없는 사용자") {
            it("빈 목록을 반환") {
                val userId = 999L

                every { mockStockReservationService.getUserReservations(userId) } returns emptyList()

                val result = sut.execute(userId)

                result shouldBe emptyList()
                verify(exactly = 1) { mockStockReservationService.getUserReservations(userId) }
            }
        }

        context("여러 상태의 예약이 있는 사용자") {
            it("서비스에서 반환하는 예약들을 그대로 반환") {
                val userId = 1L
                val expectedReservations = listOf(
                    createMockReservation(id = 1L, userId = userId, status = ReservationStatus.RESERVED),
                    createMockReservation(id = 2L, userId = userId, status = ReservationStatus.CONFIRMED),
                    createMockReservation(id = 3L, userId = userId, status = ReservationStatus.CANCELLED)
                )

                every { mockStockReservationService.getUserReservations(userId) } returns expectedReservations

                val result = sut.execute(userId)

                result shouldBe expectedReservations
                result.size shouldBe 3
                verify(exactly = 1) { mockStockReservationService.getUserReservations(userId) }
            }
        }
    }
})