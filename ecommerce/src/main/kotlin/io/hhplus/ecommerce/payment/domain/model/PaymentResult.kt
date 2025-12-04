package io.hhplus.ecommerce.payment.domain.model

/**
 * 결제 실행 결과 VO
 *
 * 역할:
 * - PaymentExecutor.execute() 반환 타입
 * - 결제 성공/실패 정보 및 외부 거래 ID 전달
 *
 * 특징:
 * - 불변 객체 (data class)
 * - 팩토리 메서드로 성공/실패 인스턴스 생성
 */
data class PaymentResult(
    val success: Boolean,
    val externalTransactionId: String? = null,
    val message: String? = null,
    val failureReason: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 결제 성공 여부
     */
    fun isSuccess(): Boolean = success

    /**
     * 결제 실패 여부
     */
    fun isFailure(): Boolean = !success

    companion object {
        /**
         * 결제 성공 결과 생성
         *
         * @param externalTransactionId 외부 거래 ID (외부 PG 사용 시)
         * @param message 성공 메시지
         */
        fun success(
            externalTransactionId: String? = null,
            message: String = "결제가 완료되었습니다"
        ): PaymentResult = PaymentResult(
            success = true,
            externalTransactionId = externalTransactionId,
            message = message
        )

        /**
         * 결제 실패 결과 생성
         *
         * @param reason 실패 사유
         */
        fun failure(reason: String): PaymentResult = PaymentResult(
            success = false,
            failureReason = reason
        )
    }
}
