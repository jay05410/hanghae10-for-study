package io.hhplus.ecommerce.inventory.controller

import io.hhplus.ecommerce.inventory.usecase.*
import io.hhplus.ecommerce.inventory.dto.*
import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "재고 관리", description = "재고 예약, 확정, 취소 API")
@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val inventoryReservationUseCase: InventoryReservationUseCase,
    private val getUserReservationsUseCase: GetUserReservationsUseCase
) {

    @Operation(summary = "재고 예약", description = "상품의 재고를 예약합니다.")
    @PostMapping("/products/{productId}/reserve")
    fun reserveStock(
        @Parameter(description = "예약할 상품 ID", required = true)
        @PathVariable productId: Long,
        @Parameter(description = "재고 예약 정보", required = true)
        @RequestBody request: ReserveStockRequest,
        @Parameter(description = "사용자 ID", required = true)
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<StockReservationResponse> {
        val reservation = inventoryReservationUseCase.reserveStock(
            productId = productId,
            userId = userId,
            quantity = request.quantity,
            reservationMinutes = request.reservationMinutes ?: 20
        )
        return ApiResponse.success(reservation.toResponse())
    }

    @Operation(summary = "재고 예약 확정", description = "재고 예약을 확정합니다.")
    @PostMapping("/reservations/{reservationId}/confirm")
    fun confirmReservation(
        @Parameter(description = "확정할 예약 ID", required = true)
        @PathVariable reservationId: Long,
        @Parameter(description = "사용자 ID", required = true)
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<StockReservationResponse> {
        val reservation = inventoryReservationUseCase.confirmReservation(reservationId, userId)
        return ApiResponse.success(reservation.toResponse())
    }

    @Operation(summary = "재고 예약 취소", description = "재고 예약을 취소합니다.")
    @PostMapping("/reservations/{reservationId}/cancel")
    fun cancelReservation(
        @Parameter(description = "취소할 예약 ID", required = true)
        @PathVariable reservationId: Long,
        @Parameter(description = "사용자 ID", required = true)
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<StockReservationResponse> {
        val reservation = inventoryReservationUseCase.cancelReservation(reservationId, userId)
        return ApiResponse.success(reservation.toResponse())
    }

    @Operation(summary = "재고 예약 내역 조회", description = "사용자의 모든 재고 예약 내역을 조회합니다.")
    @GetMapping("/reservations")
    fun getUserReservations(
        @Parameter(description = "사용자 ID", required = true)
        @RequestHeader("User-Id") userId: Long
    ): ApiResponse<List<StockReservationResponse>> {
        val reservations = getUserReservationsUseCase.execute(userId)
        return ApiResponse.success(reservations.map { it.toResponse() })
    }
}