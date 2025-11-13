package io.hhplus.ecommerce.point.controller

import io.hhplus.ecommerce.point.usecase.*
import io.hhplus.ecommerce.point.dto.*
import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * 포인트 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 포인트 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@Tag(name = "포인트 관리", description = "사용자 포인트 조회, 적립, 사용 API")
@RestController
@RequestMapping("/api/v1/points")
class PointController(
    private val getPointQueryUseCase: GetPointQueryUseCase,
    private val pointCommandUseCase: PointCommandUseCase
) {

    /**
     * 사용자의 포인트 잔액을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 포인트 정보를 포함한 API 응답
     */
    @Operation(summary = "포인트 잔액 조회", description = "특정 사용자의 현재 포인트 잔액을 조회합니다.")
    @GetMapping("/{userId}")
    fun getUserPoint(
        @Parameter(description = "조회할 사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<UserPointResponse> {
        val userPoint = getPointQueryUseCase.getUserPoint(userId)
        return ApiResponse.success(userPoint.toResponse())
    }

    /**
     * 사용자의 포인트를 적립한다
     *
     * @param userId 포인트를 적립할 사용자 ID
     * @param request 포인트 적립 요청 데이터
     * @return 업데이트된 사용자 포인트 정보를 포함한 API 응답
     */
    @Operation(summary = "포인트 적립", description = "사용자의 포인트를 적립합니다.")
    @PostMapping("/{userId}/charge")
    fun earnPoint(
        @Parameter(description = "포인트를 적립할 사용자 ID", required = true)
        @PathVariable userId: Long,
        @Parameter(description = "포인트 적립 요청 정보", required = true)
        @RequestBody request: ChargePointRequest
    ): ApiResponse<UserPointResponse> {
        val userPoint = pointCommandUseCase.chargePoint(userId, request.amount, request.description)
        return ApiResponse.success(userPoint.toResponse())
    }

    /**
     * 사용자의 포인트를 사용한다
     *
     * @param userId 포인트를 사용할 사용자 ID
     * @param request 포인트 사용 요청 데이터
     * @return 업데이트된 사용자 포인트 정보를 포함한 API 응답
     */
    @Operation(summary = "포인트 사용", description = "사용자의 포인트를 사용합니다.")
    @PostMapping("/{userId}/deduct")
    fun usePoint(
        @Parameter(description = "포인트를 사용할 사용자 ID", required = true)
        @PathVariable userId: Long,
        @Parameter(description = "포인트 사용 요청 정보", required = true)
        @RequestBody request: DeductPointRequest
    ): ApiResponse<UserPointResponse> {
        val userPoint = pointCommandUseCase.usePoint(userId, request.amount, request.description)
        return ApiResponse.success(userPoint.toResponse())
    }

    /**
     * 사용자의 포인트 사용 내역을 조회한다
     *
     * @param userId 내역을 조회할 사용자 ID
     * @return 사용자의 포인트 사용 내역 목록을 포함한 API 응답
     */
    @Operation(summary = "포인트 내역 조회", description = "사용자의 포인트 적립/사용 내역을 조회합니다.")
    @GetMapping("/{userId}/histories")
    fun getPointHistories(
        @Parameter(description = "내역을 조회할 사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<List<PointHistoryResponse>> {
        val histories = getPointQueryUseCase.getPointHistories(userId)
        return ApiResponse.success(histories.map { it.toResponse() })
    }
}