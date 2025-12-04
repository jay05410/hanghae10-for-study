package io.hhplus.ecommerce.coupon.presentation.controller

import io.hhplus.ecommerce.coupon.application.usecase.CouponCommandUseCase
import io.hhplus.ecommerce.coupon.application.usecase.GetCouponQueryUseCase
import io.hhplus.ecommerce.coupon.presentation.dto.*
import io.hhplus.ecommerce.common.response.ApiResponse
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * 쿠폰 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 쿠폰 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 */
@Tag(name = "쿠폰", description = "쿠폰 발급 및 조회 API")
@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val couponCommandUseCase: CouponCommandUseCase,
    private val getCouponQueryUseCase: GetCouponQueryUseCase
) {

    /**
     * 사용 가능한 모든 쿠폰을 조회한다
     */
    @Operation(summary = "사용 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 모든 쿠폰 목록을 조회합니다.")
    @GetMapping
    fun getAvailableCoupons(): ApiResponse<List<CouponResponse>> {
        val coupons = getCouponQueryUseCase.getAvailableCoupons()
        return ApiResponse.success(coupons.map { it.toResponse() })
    }

    /**
     * 사용자의 쿠폰 발급 요청을 등록한다
     */
    @Operation(
        summary = "쿠폰 발급 요청",
        description = "사용자의 쿠폰 발급 요청을 등록합니다. SADD 원자성으로 중복 발급을 방지합니다."
    )
    @PostMapping("/issue")
    fun issueCoupon(
        @Parameter(description = "쿠폰을 발급받을 사용자 ID", required = true, example = "1")
        @RequestParam userId: Long,
        @Parameter(description = "쿠폰 발급 요청 정보", required = true)
        @RequestBody request: IssueCouponRequest
    ): ApiResponse<CouponIssueStatusResponse> {
        couponCommandUseCase.issueCoupon(userId, request)

        // 발급 상태 조회
        val status = CouponIssueStatusResponse(
            couponId = request.couponId,
            requested = true,
            issuedCount = couponCommandUseCase.getIssuedCount(request.couponId),
            pendingCount = couponCommandUseCase.getPendingCount(request.couponId)
        )
        return ApiResponse.success(status)
    }

    /**
     * 특정 사용자의 모든 쿠폰을 조회한다
     */
    @Operation(summary = "사용자 쿠폰 목록 조회", description = "특정 사용자가 보유한 모든 쿠폰을 조회합니다.")
    @GetMapping("/users/{userId}")
    fun getUserCoupons(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        @PathVariable userId: Long
    ): ApiResponse<List<UserCouponResponse>> {
        val userCoupons = getCouponQueryUseCase.getUserCoupons(userId)
        return ApiResponse.success(userCoupons.map { it.toResponse() })
    }

    /**
     * 사용 가능한 사용자 쿠폰 조회
     */
    @Operation(summary = "사용 가능한 사용자 쿠폰 조회", description = "특정 사용자가 현재 사용 가능한 쿠폰 목록을 조회합니다.")
    @GetMapping("/users/{userId}/available")
    fun getAvailableUserCoupons(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        @PathVariable userId: Long
    ): ApiResponse<List<UserCouponResponse>> {
        val userCoupons = getCouponQueryUseCase.getUserCoupons(userId, onlyAvailable = true)
        return ApiResponse.success(userCoupons.map { it.toResponse() })
    }

    /**
     * 쿠폰 사용 유효성을 검증하고 할인 금액을 계산한다
     */
    @Operation(summary = "쿠폰 유효성 검증", description = "쿠폰 사용 가능 여부를 검증하고 할인 금액을 계산합니다.")
    @PostMapping("/validate")
    fun validateCoupon(
        @Parameter(description = "쿠폰을 사용할 사용자 ID", required = true, example = "1")
        @RequestParam userId: Long,
        @Parameter(description = "쿠폰 사용 요청 정보", required = true)
        @RequestBody request: UseCouponRequest
    ): ApiResponse<Long> {
        val discountAmount = couponCommandUseCase.validateCoupon(userId, request)
        return ApiResponse.success(discountAmount)
    }

    /**
     * 특정 쿠폰의 발급 상태를 조회한다
     */
    @Operation(summary = "쿠폰 발급 상태 조회", description = "특정 쿠폰의 현재 발급 상태를 조회합니다.")
    @GetMapping("/{couponId}/status")
    fun getCouponIssueStatus(
        @Parameter(description = "쿠폰 ID", required = true, example = "1")
        @PathVariable couponId: Long,
        @Parameter(description = "사용자 ID", required = false, example = "1")
        @RequestParam(required = false) userId: Long?
    ): ApiResponse<CouponIssueStatusResponse> {
        val requested = if (userId != null) {
            getCouponQueryUseCase.isUserRequested(couponId, userId)
        } else {
            false
        }

        val status = CouponIssueStatusResponse(
            couponId = couponId,
            requested = requested,
            issuedCount = couponCommandUseCase.getIssuedCount(couponId),
            pendingCount = couponCommandUseCase.getPendingCount(couponId)
        )
        return ApiResponse.success(status)
    }

    /**
     * 특정 쿠폰의 현재 대기열 크기를 조회한다
     */
    @Operation(summary = "대기열 크기 조회", description = "특정 쿠폰의 현재 대기 중인 발급 요청 수를 조회합니다.")
    @GetMapping("/issue/coupon/{couponId}/pending")
    fun getPendingCount(
        @Parameter(description = "쿠폰 ID", required = true, example = "1")
        @PathVariable couponId: Long
    ): ApiResponse<Long> {
        val pendingCount = couponCommandUseCase.getPendingCount(couponId)
        return ApiResponse.success(pendingCount)
    }
}