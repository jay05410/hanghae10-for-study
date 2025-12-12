package io.hhplus.ecommerce.product.domain.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 상품 통계 이벤트 - 단방향 흐름용
 *
 * 특징:
 * - Write-back 방식 제거
 * - 이벤트 로그로 쌓아서 청크 단위 처리
 * - 단방향: 이벤트 발생 → 로그 저장 → 배치 집계 → DB 업데이트
 */
@Serializable
sealed class ProductStatisticsEvent {
    abstract val productId: Long
    abstract val timestamp: Long

    /**
     * 상품 조회 이벤트
     */
    @Serializable
    @SerialName("ProductViewed")
    data class ProductViewed(
        val pId: Long,
        val userId: Long? = null,
        val sessionId: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProductStatisticsEvent() {
        override val productId: Long get() = pId
    }

    /**
     * 상품 판매 이벤트
     */
    @Serializable
    @SerialName("ProductSold")
    data class ProductSold(
        val pId: Long,
        val quantity: Int,
        val orderId: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProductStatisticsEvent() {
        override val productId: Long get() = pId

        init {
            require(quantity > 0) { "판매 수량은 양수여야 합니다" }
        }
    }

    /**
     * 상품 찜 이벤트
     */
    @Serializable
    @SerialName("ProductWished")
    data class ProductWished(
        val pId: Long,
        val userId: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProductStatisticsEvent() {
        override val productId: Long get() = pId
    }

    /**
     * 상품 찜 해제 이벤트
     */
    @Serializable
    @SerialName("ProductUnwished")
    data class ProductUnwished(
        val pId: Long,
        val userId: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProductStatisticsEvent() {
        override val productId: Long get() = pId
    }
}