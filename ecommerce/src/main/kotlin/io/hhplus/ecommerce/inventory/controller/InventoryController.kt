package io.hhplus.ecommerce.inventory.controller

import io.hhplus.ecommerce.inventory.usecase.*
import io.hhplus.ecommerce.inventory.dto.*
import io.hhplus.ecommerce.inventory.dto.StockReservationResponse.Companion.toResponse
import io.hhplus.ecommerce.common.response.ApiResponse
import org.springframework.web.bind.annotation.*

/**
 * 재고 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 재고 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val reserveStockUseCase: ReserveStockUseCase,
    private val confirmReservationUseCase: ConfirmReservationUseCase,
    private val cancelReservationUseCase: CancelReservationUseCase,
    private val getUserReservationsUseCase: GetUserReservationsUseCase
) {

    /**
     * 상품의 재고를 예약한다
     *
     * @param productId 예약할 상품 ID
     * @param request 재고 예약 요청 데이터
     * @param userId 예약을 요청할 사용자 ID
     * @return 생성된 재고 예약 정보를 포함한 API 응답
     */
    @PostMapping("/products/{productId}/reserve")
    fun reserveStock(
        @PathVariable productId: Long,
        @RequestBody request: ReserveStockRequest,
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<StockReservationResponse> {
        val reservation = reserveStockUseCase.execute(
            productId = productId,
            userId = userId,
            quantity = request.quantity,
            reservationMinutes = request.reservationMinutes ?: 20
        )
        return ApiResponse.success(reservation.toResponse())
    }

    /**
     * 재고 예약을 확정한다
     *
     * @param reservationId 확정할 예약 ID
     * @param userId 예약을 확정할 사용자 ID
     * @return 확정된 재고 예약 정보를 포함한 API 응답
     */
    @PostMapping("/reservations/{reservationId}/confirm")
    fun confirmReservation(
        @PathVariable reservationId: Long,
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<StockReservationResponse> {
        val reservation = confirmReservationUseCase.execute(reservationId, userId)
        return ApiResponse.success(reservation.toResponse())
    }

    /**
     * 재고 예약을 취소한다
     *
     * @param reservationId 취소할 예약 ID
     * @param userId 예약을 취소할 사용자 ID
     * @return 취소된 재고 예약 정보를 포함한 API 응답
     */
    @PostMapping("/reservations/{reservationId}/cancel")
    fun cancelReservation(
        @PathVariable reservationId: Long,
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<StockReservationResponse> {
        val reservation = cancelReservationUseCase.execute(reservationId, userId)
        return ApiResponse.success(reservation.toResponse())
    }

    /**
     * 사용자의 모든 재고 예약 내역을 조회한다
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 재고 예약 목록을 포함한 API 응답
     */
    @GetMapping("/reservations")
    fun getUserReservations(@RequestHeader("User-Id") userId: Long): ApiResponse<List<StockReservationResponse>> {
        val reservations = getUserReservationsUseCase.execute(userId)
        return ApiResponse.success(reservations.map { it.toResponse() })
    }
}