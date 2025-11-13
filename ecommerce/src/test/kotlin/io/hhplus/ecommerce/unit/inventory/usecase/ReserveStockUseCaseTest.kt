package io.hhplus.ecommerce.unit.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * ReserveStockUseCase 단위 테스트
 *
 * 책임: 재고 예약 유스케이스의 비즈니스 로직 검증
 * - 서비스 호출 및 결과 반환 검증
 */
class ReserveStockUseCaseTest : DescribeSpec({
    val mockStockReservationService = mockk<StockReservationService>()
    val sut = ReserveStockUseCase(mockStockReservationService)

    fun createMockStockReservation(
        id: Long = 1L,
        productId: Long = 1L,
        userId: Long = 1L,
        quantity: Int = 5
    ): StockReservation = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.productId } returns productId
        every { this@mockk.userId } returns userId
        every { this@mockk.quantity } returns quantity
        every { this@mockk.status } returns ReservationStatus.RESERVED
        every { createdAt } returns LocalDateTime.now()
        every { expiresAt } returns LocalDateTime.now().plusMinutes(20)
    }

    beforeEach {
        clearMocks(mockStockReservationService)
    }

    describe("execute") {
        context("정상적인 재고 예약 요청") {
            it("StockReservationService를 호출하고 결과를 반환") {
                val productId = 1L
                val userId = 1L
                val quantity = 5
                val reservationMinutes = 30
                val mockReservation = createMockStockReservation(productId = productId, userId = userId, quantity = quantity)

                every { mockStockReservationService.reserveStock(productId, userId, quantity, reservationMinutes) } returns mockReservation

                val result = sut.execute(productId, userId, quantity, reservationMinutes)

                result shouldBe mockReservation
                verify(exactly = 1) { mockStockReservationService.reserveStock(productId, userId, quantity, reservationMinutes) }
            }
        }

        context("기본 예약 시간 사용") {
            it("기본값으로 서비스를 호출") {
                val productId = 2L
                val userId = 2L
                val quantity = 3
                val reservationMinutes = 20
                val mockReservation = createMockStockReservation(productId = productId, userId = userId, quantity = quantity)

                every { mockStockReservationService.reserveStock(productId, userId, quantity, reservationMinutes) } returns mockReservation

                val result = sut.execute(productId, userId, quantity, reservationMinutes)

                result shouldBe mockReservation
                verify(exactly = 1) { mockStockReservationService.reserveStock(productId, userId, quantity, reservationMinutes) }
            }
        }
    }
})