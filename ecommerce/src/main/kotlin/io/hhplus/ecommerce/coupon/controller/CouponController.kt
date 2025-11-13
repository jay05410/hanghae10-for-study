package io.hhplus.ecommerce.coupon.controller

import io.hhplus.ecommerce.coupon.usecase.*
import io.hhplus.ecommerce.coupon.dto.*
import io.hhplus.ecommerce.common.response.ApiResponse
import org.springframework.web.bind.annotation.*

/**
 * 쿠폰 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 쿠폰 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "쿠폰", description = "쿠폰 발급 및 조회 API")
@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val getCouponQueryUseCase: GetCouponQueryUseCase,
    private val issueCouponUseCase: IssueCouponUseCase,
    private val validateCouponUseCase: ValidateCouponUseCase
) {

    /**
     * 사용 가능한 모든 쿠폰을 조회한다
     *
     * @return 사용 가능한 쿠폰 목록을 포함한 API 응답
     */
    @Operation(summary = "사용 가능한 쿠폰 목록 조회", description = "현재 발급 가능한 모든 쿠폰 목록을 조회합니다.")
    @GetMapping
    fun getAvailableCoupons(): ApiResponse<List<CouponResponse>> {
        val coupons = getCouponQueryUseCase.getAvailableCoupons()
        return ApiResponse.success(coupons.map { it.toResponse() })
    }

    /**
     * 사용자에게 쿠폰을 발급한다
     *
     * @param userId 쿠폰을 발급받을 사용자 ID
     * @param request 쿠폰 발급 요청 데이터
     * @return 발급된 사용자 쿠폰 정보를 포함한 API 응답
     */
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다.")
    @PostMapping("/issue")
    fun issueCoupon(
        @Parameter(description = "쿠폰을 발급받을 사용자 ID", required = true, example = "1")
        @RequestParam userId: Long,
        @Parameter(description = "쿠폰 발급 요청 정보", required = true)
        @RequestBody request: IssueCouponRequest
    ): ApiResponse<UserCouponResponse> {
        val userCoupon = issueCouponUseCase.execute(userId, request)
        return ApiResponse.success(userCoupon.toResponse())
    }

    /**
     * 특정 사용자의 모든 쿠폰을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자가 보유한 모든 쿠폰 목록을 포함한 API 응답
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
     * 특정 사용자의 사용 가능한 쿠폰을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자가 사용 가능한 쿠폰 목록을 포함한 API 응답
     */
    @Operation(summary = "사용 가능한 사용자 쿠폰 조회", description = "특정 사용자가 현재 사용 가능한 쿠폰 목록을 조회합니다.")
    @GetMapping("/users/{userId}/available")
    fun getAvailableUserCoupons(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        @PathVariable userId: Long
    ): ApiResponse<List<UserCouponResponse>> {
        val userCoupons = getCouponQueryUseCase.getAvailableUserCoupons(userId)
        return ApiResponse.success(userCoupons.map { it.toResponse() })
    }

    /**
     * 쿠폰 사용 유효성을 검증하고 할인 금액을 계산한다
     *
     * @param userId 쿠폰을 사용할 사용자 ID
     * @param request 쿠폰 사용 요청 데이터
     * @return 계산된 할인 금액을 포함한 API 응답
     */
    @Operation(summary = "쿠폰 유효성 검증", description = "쿠폰 사용 가능 여부를 검증하고 할인 금액을 계산합니다.")
    @PostMapping("/validate")
    fun validateCoupon(
        @Parameter(description = "쿠폰을 사용할 사용자 ID", required = true, example = "1")
        @RequestParam userId: Long,
        @Parameter(description = "쿠폰 사용 요청 정보", required = true)
        @RequestBody request: UseCouponRequest
    ): ApiResponse<Long> {
        val discountAmount = validateCouponUseCase.execute(userId, request)
        return ApiResponse.success(discountAmount)
    }
}