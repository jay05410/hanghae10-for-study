package io.hhplus.ecommerce.inventory.controller

import io.hhplus.ecommerce.inventory.usecase.*
import io.hhplus.ecommerce.inventory.dto.*
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * InventoryController 단위 테스트
 *
 * 책임: 재고 컨트롤러의 HTTP 요청/응답 처리 검증
 * - REST API 엔드포인트 동작 검증
 * - UseCase와의 상호작용 검증
 */
class InventoryControllerTest : DescribeSpec({
    val mockReserveStockUseCase = mockk<ReserveStockUseCase>()
    val mockConfirmReservationUseCase = mockk<ConfirmReservationUseCase>()
    val mockCancelReservationUseCase = mockk<CancelReservationUseCase>()
    val mockGetUserReservationsUseCase = mockk<GetUserReservationsUseCase>()

    val sut = InventoryController(
        reserveStockUseCase = mockReserveStockUseCase,
        confirmReservationUseCase = mockConfirmReservationUseCase,
        cancelReservationUseCase = mockCancelReservationUseCase,
        getUserReservationsUseCase = mockGetUserReservationsUseCase
    )

    fun createMockStockReservation(
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
        clearMocks(
            mockReserveStockUseCase,
            mockConfirmReservationUseCase,
            mockCancelReservationUseCase,
            mockGetUserReservationsUseCase
        )
    }

    describe("reserveStock") {
        context("POST /api/v1/inventory/products/{productId}/reserve 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val productId = 1L
                val userId = 1L
                val request = ReserveStockRequest(quantity = 5, reservationMinutes = 20)
                val mockReservation = createMockStockReservation(productId = productId, userId = userId, quantity = 5)

                every { mockReserveStockUseCase.execute(productId, userId, 5, 20) } returns mockReservation

                val result = sut.reserveStock(productId, request, userId)

                result shouldBe ApiResponse.success(StockReservationResponse.from(mockReservation))
                verify(exactly = 1) { mockReserveStockUseCase.execute(productId, userId, 5, 20) }
            }
        }

        context("예약 시간이 지정되지 않은 경우") {
            it("기본 20분으로 설정하여 UseCase 호출") {
                val productId = 1L
                val userId = 1L
                val request = ReserveStockRequest(quantity = 3, reservationMinutes = null)
                val mockReservation = createMockStockReservation(productId = productId, userId = userId, quantity = 3)

                every { mockReserveStockUseCase.execute(productId, userId, 3, 20) } returns mockReservation

                val result = sut.reserveStock(productId, request, userId)

                result shouldBe ApiResponse.success(StockReservationResponse.from(mockReservation))
                verify(exactly = 1) { mockReserveStockUseCase.execute(productId, userId, 3, 20) }
            }
        }
    }

    describe("confirmReservation") {
        context("POST /api/v1/inventory/reservations/{reservationId}/confirm 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val reservationId = 1L
                val userId = 1L
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, status = ReservationStatus.CONFIRMED)

                every { mockConfirmReservationUseCase.execute(reservationId, userId) } returns mockReservation

                val result = sut.confirmReservation(reservationId, userId)

                result shouldBe ApiResponse.success(StockReservationResponse.from(mockReservation))
                verify(exactly = 1) { mockConfirmReservationUseCase.execute(reservationId, userId) }
            }
        }
    }

    describe("cancelReservation") {
        context("POST /api/v1/inventory/reservations/{reservationId}/cancel 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val reservationId = 1L
                val userId = 1L
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, status = ReservationStatus.CANCELLED)

                every { mockCancelReservationUseCase.execute(reservationId, userId) } returns mockReservation

                val result = sut.cancelReservation(reservationId, userId)

                result shouldBe ApiResponse.success(StockReservationResponse.from(mockReservation))
                verify(exactly = 1) { mockCancelReservationUseCase.execute(reservationId, userId) }
            }
        }
    }

    describe("getUserReservations") {
        context("GET /api/v1/inventory/reservations 요청") {
            it("UseCase를 호출하고 결과를 ApiResponse로 감싸서 반환") {
                val userId = 1L
                val mockReservations = listOf(
                    createMockStockReservation(id = 1L, userId = userId),
                    createMockStockReservation(id = 2L, userId = userId)
                )

                every { mockGetUserReservationsUseCase.execute(userId) } returns mockReservations

                val result = sut.getUserReservations(userId)

                val expectedResponses = mockReservations.map { StockReservationResponse.from(it) }
                result shouldBe ApiResponse.success(expectedResponses)
                verify(exactly = 1) { mockGetUserReservationsUseCase.execute(userId) }
            }
        }

        context("사용자의 예약이 없는 경우") {
            it("빈 목록을 ApiResponse로 감싸서 반환") {
                val userId = 999L

                every { mockGetUserReservationsUseCase.execute(userId) } returns emptyList()

                val result = sut.getUserReservations(userId)

                result shouldBe ApiResponse.success(emptyList<StockReservationResponse>())
                verify(exactly = 1) { mockGetUserReservationsUseCase.execute(userId) }
            }
        }
    }
})