package io.hhplus.ecommerce.payment.controller

import io.hhplus.ecommerce.payment.usecase.*
import io.hhplus.ecommerce.payment.dto.*
import io.hhplus.ecommerce.payment.dto.PaymentResponse.Companion.toResponse
import io.hhplus.ecommerce.payment.domain.*
import io.hhplus.ecommerce.common.response.ApiResponse
import io.hhplus.ecommerce.payment.domain.entity.Payment
import org.springframework.web.bind.annotation.*

/**
 * 결제 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 결제 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val getPaymentQueryUseCase: GetPaymentQueryUseCase
) {

    /**
     * 결제를 처리한다
     *
     * @param request 결제 처리 요청 데이터
     * @return 처리된 결제 정보를 포함한 API 응답
     */
    @PostMapping("/process")
    fun processPayment(@RequestBody request: ProcessPaymentRequest): ApiResponse<PaymentResponse> {
        val payment = processPaymentUseCase.execute(request)
        return ApiResponse.success(payment.toResponse())
    }

    /**
     * 결제 ID로 단일 결제 내역을 조회한다
     *
     * @param paymentId 조회할 결제의 ID
     * @return 결제 정보를 포함한 API 응답
     */
    @GetMapping("/{paymentId}")
    fun getPayment(@PathVariable paymentId: Long): ApiResponse<PaymentResponse?> {
        val payment = getPaymentQueryUseCase.getPayment(paymentId)
        return ApiResponse.success(payment?.toResponse())
    }

    /**
     * 특정 사용자의 모든 결제 내역을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 결제 내역 목록을 포함한 API 응답
     */
    @GetMapping("/users/{userId}")
    fun getUserPayments(@PathVariable userId: Long): ApiResponse<List<PaymentResponse>> {
        val payments = getPaymentQueryUseCase.getUserPayments(userId)
        return ApiResponse.success(payments.map { it.toResponse() })
    }
}