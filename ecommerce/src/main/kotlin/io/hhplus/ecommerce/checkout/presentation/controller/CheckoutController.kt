package io.hhplus.ecommerce.checkout.presentation.controller

import io.hhplus.ecommerce.checkout.application.service.CheckoutQueueResponse
import io.hhplus.ecommerce.checkout.application.service.CheckoutQueueService
import io.hhplus.ecommerce.checkout.application.usecase.CheckoutUseCase
import io.hhplus.ecommerce.checkout.presentation.dto.CancelCheckoutRequest
import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutRequest
import io.hhplus.ecommerce.checkout.presentation.dto.toResponse
import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * 체크아웃 API 컨트롤러
 *
 * 체크아웃 = 결제 버튼 클릭 시점, 재고 락 확보
 *
 * 엔드포인트:
 * - POST /api/v1/checkout: 체크아웃 (비동기 + 자동 Failover)
 * - POST /api/v1/checkout/{orderId}/cancel: 체크아웃 취소
 * - GET /api/v1/checkout/status/{requestId}: 대기열 상태 조회
 */
@Tag(name = "체크아웃", description = "체크아웃 (결제 진입) API")
@RestController
@RequestMapping("/api/v1/checkout")
class CheckoutController(
    private val checkoutUseCase: CheckoutUseCase,
    private val checkoutQueueService: CheckoutQueueService
) {

    /**
     * 체크아웃 (결제 진입)
     *
     * 체크아웃 = 결제 버튼 클릭 → 재고 확보 시점
     *
     * 처리 흐름:
     * 1. 정상: Kafka 큐 발행 → 대기열 순번 반환 → SSE로 결과 푸시
     * 2. Failover: Kafka 실패 시 → 동기 처리 폴백 → 즉시 결과 반환
     *
     * @param request 체크아웃 요청 (장바구니 ID 또는 상품 목록)
     * @return 정상: 대기열 정보 / Failover: 체크아웃 결과
     */
    @Operation(
        summary = "체크아웃",
        description = "결제 진입. Kafka 비동기 처리, 실패 시 자동 동기 폴백."
    )
    @PostMapping
    fun checkout(
        @Parameter(description = "체크아웃 요청", required = true)
        @RequestBody request: CheckoutRequest
    ): ApiResponse<Any> {
        request.validate()

        return try {
            // 1차: Kafka 큐 발행 시도
            val queueResponse = checkoutQueueService.enqueue(request)
            ApiResponse.success(queueResponse as Any)
        } catch (e: Exception) {
            // 2차: Failover - 동기 처리로 폴백
            logger.warn { "[Checkout Failover] Kafka 실패, 동기 폴백: ${e.message}" }
            val checkoutSession = checkoutUseCase.processCheckout(request)
            ApiResponse.success(checkoutSession.toResponse() as Any)
        }
    }

    /**
     * 체크아웃 취소
     *
     * 결제 페이지 이탈 시 호출
     * - 재고 예약 해제
     * - 주문 상태 EXPIRED로 변경
     */
    @Operation(
        summary = "체크아웃 취소",
        description = "결제 이탈 시 호출. 재고 예약 해제."
    )
    @PostMapping("/{orderId}/cancel")
    fun cancelCheckout(
        @Parameter(description = "주문 ID", required = true)
        @PathVariable orderId: Long,
        @Parameter(description = "취소 요청", required = true)
        @RequestBody request: CancelCheckoutRequest
    ): ApiResponse<Unit> {
        checkoutUseCase.cancelCheckout(orderId, request.userId)
        return ApiResponse.success(Unit)
    }

    /**
     * 대기열 상태 조회
     */
    @Operation(
        summary = "대기열 상태 조회",
        description = "체크아웃 대기열 순번 조회"
    )
    @GetMapping("/status/{requestId}")
    fun getQueueStatus(
        @Parameter(description = "요청 ID", required = true)
        @PathVariable requestId: String
    ): ApiResponse<Map<String, Any?>> {
        val position = checkoutQueueService.getQueuePosition(requestId)
        return ApiResponse.success(
            mapOf(
                "requestId" to requestId,
                "queuePosition" to position,
                "status" to if (position != null) "WAITING" else "COMPLETED_OR_NOT_FOUND"
            )
        )
    }
}
