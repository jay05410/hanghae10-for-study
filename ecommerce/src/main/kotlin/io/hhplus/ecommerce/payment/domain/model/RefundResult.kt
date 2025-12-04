package io.hhplus.ecommerce.payment.domain.model

/**
 * 환불 실행 결과 VO
 *
 * 역할:
 * - PaymentExecutor.refund() 반환 타입
 * - 환불 성공/실패 정보 및 환불 금액 전달
 *
 * 특징:
 * - 불변 객체 (data class)
 * - 팩토리 메서드로 성공/실패 인스턴스 생성
 */
data class RefundResult(
    val success: Boolean,
    val refundTransactionId: String? = null,
    val refundedAmount: Long = 0,
    val message: String? = null,
    val failureReason: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 환불 성공 여부
     */
    fun isSuccess(): Boolean = success

    /**
     * 환불 실패 여부
     */
    fun isFailure(): Boolean = !success

    companion object {
        /**
         * 환불 성공 결과 생성
         *
         * @param refundTransactionId 환불 거래 ID
         * @param refundedAmount 환불된 금액
         * @param message 성공 메시지
         */
        fun success(
            refundTransactionId: String? = null,
            refundedAmount: Long,
            message: String = "환불이 완료되었습니다"
        ): RefundResult = RefundResult(
            success = true,
            refundTransactionId = refundTransactionId,
            refundedAmount = refundedAmount,
            message = message
        )

        /**
         * 환불 실패 결과 생성
         *
         * @param reason 실패 사유
         */
        fun failure(reason: String): RefundResult = RefundResult(
            success = false,
            failureReason = reason
        )
    }
}
