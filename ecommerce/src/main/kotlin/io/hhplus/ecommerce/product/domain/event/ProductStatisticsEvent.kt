package io.hhplus.ecommerce.product.domain.event

/**
 * 상품 통계 이벤트 - 단방향 흐름용
 *
 * 특징:
 * - Write-back 방식 제거
 * - 이벤트 로그로 쌓아서 청크 단위 처리
 * - 단방향: 이벤트 발생 → 로그 저장 → 배치 집계 → DB 업데이트
 */
sealed class ProductStatisticsEvent(
    val productId: Long,
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * 상품 조회 이벤트
     */
    data class ProductViewed(
        val pId: Long,
        val userId: Long? = null,
        val sessionId: String? = null
    ) : ProductStatisticsEvent(pId)

    /**
     * 상품 판매 이벤트
     */
    data class ProductSold(
        val pId: Long,
        val quantity: Int,
        val orderId: Long
    ) : ProductStatisticsEvent(pId) {
        init {
            require(quantity > 0) { "판매 수량은 양수여야 합니다" }
        }
    }

    /**
     * 상품 찜 이벤트
     */
    data class ProductWished(
        val pId: Long,
        val userId: Long
    ) : ProductStatisticsEvent(pId)

    /**
     * 상품 찜 해제 이벤트
     */
    data class ProductUnwished(
        val pId: Long,
        val userId: Long
    ) : ProductStatisticsEvent(pId)
}