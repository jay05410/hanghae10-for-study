package io.hhplus.ecommerce.payment.domain.model

import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod

/**
 * 결제 컨텍스트 - 결제/환불 실행에 필요한 모든 정보를 담는 VO
 *
 * 역할:
 * - PaymentExecutor에 전달되는 결제 정보 캡슐화
 * - 결제와 환불 양쪽에서 사용 가능한 통합 컨텍스트
 *
 * 사용처:
 * - PaymentExecutor.execute() 파라미터
 * - PaymentExecutor.refund() 파라미터
 * - PaymentGateway 외부 호출 시 정보 전달
 */
data class PaymentContext(
    val userId: Long,
    val orderId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val paymentId: Long? = null,
    val externalTransactionId: String? = null,
    val description: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    init {
        require(userId > 0) { "사용자 ID는 0보다 커야 함" }
        require(orderId > 0) { "주문 ID는 0보다 커야 함" }
        require(amount > 0) { "금액은 0보다 커야 함" }
    }

    companion object {
        /**
         * 결제용 컨텍스트 생성
         */
        fun forPayment(
            userId: Long,
            orderId: Long,
            amount: Long,
            paymentMethod: PaymentMethod,
            description: String? = null
        ): PaymentContext = PaymentContext(
            userId = userId,
            orderId = orderId,
            amount = amount,
            paymentMethod = paymentMethod,
            description = description
        )

        /**
         * 환불용 컨텍스트 생성
         */
        fun forRefund(
            userId: Long,
            orderId: Long,
            amount: Long,
            paymentMethod: PaymentMethod,
            paymentId: Long,
            externalTransactionId: String? = null,
            description: String? = null
        ): PaymentContext = PaymentContext(
            userId = userId,
            orderId = orderId,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentId = paymentId,
            externalTransactionId = externalTransactionId,
            description = description
        )
    }
}
