package io.hhplus.ecommerce.product.domain.vo

/**
 * 상품 통계 Value Object
 *
 * 기존 ProductStatistics 엔티티를 대체하는 읽기 전용 데이터 클래스
 * Redis 기반 실시간 통계 데이터 표현
 */
data class ProductStatsVO(
    val productId: Long,
    val viewCount: Long,
    val salesCount: Long,
    val hotScore: Double
) {
    companion object {
        fun create(
            productId: Long,
            viewCount: Long,
            salesCount: Long,
            hotScore: Double
        ): ProductStatsVO {
            require(productId > 0) { "상품 ID는 양수여야 합니다" }
            require(viewCount >= 0) { "조회수는 0 이상이어야 합니다" }
            require(salesCount >= 0) { "판매량은 0 이상이어야 합니다" }
            require(hotScore >= 0) { "인기 점수는 0 이상이어야 합니다" }

            return ProductStatsVO(
                productId = productId,
                viewCount = viewCount,
                salesCount = salesCount,
                hotScore = hotScore
            )
        }
    }

    fun getPopularityScore(): Double = hotScore
}