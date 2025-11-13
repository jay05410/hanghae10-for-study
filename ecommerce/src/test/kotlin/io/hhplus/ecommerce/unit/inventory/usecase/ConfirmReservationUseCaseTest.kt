package io.hhplus.ecommerce.unit.inventory.usecase

import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.usecase.ConfirmReservationUseCase
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.common.exception.inventory.InventoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * ConfirmReservationUseCase 단위 테스트
 *
 * 책임: 재고 예약 확정 유스케이스의 동작 검증
 * - 예약 확정 프로세스 검증
 * - StockReservationService와의 상호작용 검증
 * - 예외 상황 처리 검증
 */
class ConfirmReservationUseCaseTest : DescribeSpec({
    val mockStockReservationService = mockk<StockReservationService>()
    val sut = ConfirmReservationUseCase(mockStockReservationService)

    fun createMockReservation(
        id: Long = 1L,
        productId: Long = 1L,
        userId: Long = 1L,
        quantity: Int = 5,
        status: ReservationStatus = ReservationStatus.CONFIRMED
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
        context("정상적인 예약 확정") {
            it("서비스를 통해 예약을 확정하고 결과를 반환") {
                val reservationId = 1L
                val userId = 1L
                val confirmedReservation = createMockReservation(id = reservationId, userId = userId)

                every { mockStockReservationService.confirmReservation(reservationId, userId) } returns confirmedReservation

                val result = sut.execute(reservationId, userId)

                result shouldBe confirmedReservation
                verify(exactly = 1) { mockStockReservationService.confirmReservation(reservationId, userId) }
            }
        }

        context("존재하지 않는 예약 확정") {
            it("ReservationNotFound 예외를 전파") {
                val reservationId = 999L
                val userId = 1L

                every {
                    mockStockReservationService.confirmReservation(reservationId, userId)
                } throws InventoryException.ReservationNotFound(reservationId)

                shouldThrow<InventoryException.ReservationNotFound> {
                    sut.execute(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationService.confirmReservation(reservationId, userId) }
            }
        }

        context("다른 사용자의 예약 확정") {
            it("ReservationAccessDenied 예외를 전파") {
                val reservationId = 1L
                val userId = 1L

                every {
                    mockStockReservationService.confirmReservation(reservationId, userId)
                } throws InventoryException.ReservationAccessDenied(reservationId, userId)

                shouldThrow<InventoryException.ReservationAccessDenied> {
                    sut.execute(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationService.confirmReservation(reservationId, userId) }
            }
        }

        context("만료된 예약 확정") {
            it("ReservationExpired 예외를 전파") {
                val reservationId = 1L
                val userId = 1L

                every {
                    mockStockReservationService.confirmReservation(reservationId, userId)
                } throws InventoryException.ReservationExpired(reservationId)

                shouldThrow<InventoryException.ReservationExpired> {
                    sut.execute(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationService.confirmReservation(reservationId, userId) }
            }
        }

        context("이미 처리된 예약") {
            it("확정된 예약 정보를 반환") {
                val reservationId = 1L
                val userId = 1L
                val alreadyConfirmedReservation = createMockReservation(
                    id = reservationId,
                    userId = userId,
                    status = ReservationStatus.CONFIRMED
                )

                every { mockStockReservationService.confirmReservation(reservationId, userId) } returns alreadyConfirmedReservation

                val result = sut.execute(reservationId, userId)

                result shouldBe alreadyConfirmedReservation
                result.status shouldBe ReservationStatus.CONFIRMED
                verify(exactly = 1) { mockStockReservationService.confirmReservation(reservationId, userId) }
            }
        }
    }
})