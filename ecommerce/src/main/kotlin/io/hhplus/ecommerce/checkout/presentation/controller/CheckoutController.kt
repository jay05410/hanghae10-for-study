package io.hhplus.ecommerce.checkout.presentation.controller

import io.hhplus.ecommerce.checkout.application.usecase.CheckoutUseCase
import io.hhplus.ecommerce.checkout.presentation.dto.CancelCheckoutRequest
import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutResponse
import io.hhplus.ecommerce.checkout.presentation.dto.InitiateCheckoutRequest
import io.hhplus.ecommerce.checkout.presentation.dto.toResponse
import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * 체크아웃 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 주문하기 관련 REST API 엔드포인트 제공
 * - 장바구니에서 주문 또는 바로 주문 처리
 *
 * 엔드포인트:
 * - POST /api/v1/checkout/initiate: 주문하기 (재고 예약 + PENDING_PAYMENT 주문 생성)
 * - POST /api/v1/checkout/{orderId}/cancel: 주문창 이탈 (재고 예약 해제 + 주문 만료)
 */
@Tag(name = "주문하기", description = "주문하기 (체크아웃) API")
@RestController
@RequestMapping("/api/v1/checkout")
class CheckoutController(
    private val checkoutUseCase: CheckoutUseCase
) {

    /**
     * 주문하기 (장바구니 또는 바로 주문)
     *
     * 처리 흐름:
     * 1. 장바구니 아이템 또는 바로 주문 상품 조회
     * 2. 가격 계산
     * 3. 재고 예약 (일반 상품: 10분, 예약 상품: 30분)
     * 4. PENDING_PAYMENT 상태의 주문 생성
     * 5. 주문창 정보 반환
     *
     * @param request 주문하기 요청 (cartItemIds 또는 directOrderItems)
     * @return 주문창 정보 (주문 ID, 만료 시간, 금액, 상품 목록 등)
     */
    @Operation(
        summary = "주문하기",
        description = "장바구니에서 주문하기 또는 상품에서 바로 주문. 재고를 예약하고 PENDING_PAYMENT 상태의 주문을 생성합니다."
    )
    @PostMapping("/initiate")
    fun initiateCheckout(
        @Parameter(description = "주문하기 요청 정보", required = true)
        @RequestBody request: InitiateCheckoutRequest
    ): ApiResponse<CheckoutResponse> {
        val checkoutSession = checkoutUseCase.initiateCheckout(request)
        return ApiResponse.success(checkoutSession.toResponse())
    }

    /**
     * 주문창 이탈 (체크아웃 취소)
     *
     * 처리 흐름:
     * 1. 재고 예약 해제
     * 2. 주문 상태를 EXPIRED로 변경
     *
     * 호출 시점:
     * - 사용자가 주문창에서 뒤로가기/닫기 클릭
     * - 프론트엔드에서 beforeunload 이벤트로 호출
     *
     * @param orderId 취소할 주문 ID
     * @param request 취소 요청 정보
     * @return 성공 응답
     */
    @Operation(
        summary = "주문창 이탈",
        description = "주문창 이탈 시 호출. 재고 예약을 해제하고 주문을 만료 처리합니다."
    )
    @PostMapping("/{orderId}/cancel")
    fun cancelCheckout(
        @Parameter(description = "주문 ID", required = true, example = "1")
        @PathVariable orderId: Long,
        @Parameter(description = "취소 요청 정보", required = true)
        @RequestBody request: CancelCheckoutRequest
    ): ApiResponse<Unit> {
        checkoutUseCase.cancelCheckout(orderId, request.userId)
        return ApiResponse.success(Unit)
    }
}
