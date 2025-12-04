package io.hhplus.ecommerce.payment.application.port.out

import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.model.RefundResult

/**
 * 결제 실행 포트 - 출력 포트 (Hexagonal Architecture)
 *
 * 역할:
 * - 결제 수단별 실행 전략 인터페이스 정의
 * - 애플리케이션 계층이 인프라에 의존하지 않도록 추상화
 * - 전략 패턴을 통한 결제 수단 확장성 제공
 *
 * 구현체:
 * - BalancePaymentExecutor: 포인트/잔액 결제 구현
 * - (추후) CardPaymentExecutor: 카드 결제 구현
 * - (추후) BankTransferPaymentExecutor: 계좌이체 결제 구현
 */
interface PaymentExecutorPort {

    /**
     * 지원하는 결제 수단 반환
     *
     * @return 해당 Executor가 지원하는 결제 수단
     */
    fun supportedMethod(): PaymentMethod

    /**
     * 결제 가능 여부 검증
     *
     * @param context 결제 컨텍스트
     * @return 결제 가능 여부
     */
    fun canExecute(context: PaymentContext): Boolean

    /**
     * 결제 실행
     *
     * @param context 결제 컨텍스트
     * @return 결제 실행 결과
     */
    fun execute(context: PaymentContext): PaymentResult

    /**
     * 환불 실행
     *
     * @param context 환불 컨텍스트
     * @return 환불 실행 결과
     */
    fun refund(context: PaymentContext): RefundResult
}
