package io.hhplus.ecommerce.payment.domain.gateway

import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.model.RefundResult

/**
 * 외부 결제 게이트웨이 포트 인터페이스
 *
 * 역할:
 * - 외부 PG사 연동 추상화
 * - 도메인 로직과 인프라(외부 API) 분리
 *
 * 책임:
 * - 외부 결제 요청 처리
 * - 외부 환불 요청 처리
 * - 결제 상태 조회
 *
 * 구현체 (향후 infra/gateway/):
 * - MockPaymentGateway: 테스트/개발용
 * - TossPaymentGateway: 토스 PG
 * - NaverPayPaymentGateway: 네이버페이
 * - KakaoPayPaymentGateway: 카카오페이
 *
 * MSA 분리 시:
 * - 이 인터페이스를 통해 외부 Payment Service 호출
 * - HTTP/gRPC 클라이언트로 구현체 교체 가능
 */
interface PaymentGateway {

    /**
     * 게이트웨이 식별자 (로깅/모니터링용)
     *
     * @return PG사 식별자 (예: "TOSS", "NAVERPAY", "KAKAOPAY")
     */
    fun gatewayId(): String

    /**
     * 외부 결제 요청
     *
     * @param context 결제 컨텍스트
     * @return 결제 결과 (외부 거래 ID 포함)
     */
    fun requestPayment(context: PaymentContext): PaymentResult

    /**
     * 외부 환불 요청
     *
     * @param context 환불 컨텍스트 (원 결제 정보 포함)
     * @return 환불 결과
     */
    fun requestRefund(context: PaymentContext): RefundResult

    /**
     * 외부 결제 상태 조회
     *
     * @param externalTransactionId 외부 거래 ID
     * @return 결제 상태 정보
     */
    fun getPaymentStatus(externalTransactionId: String): PaymentResult
}
