package io.hhplus.ecommerce.product.domain.calculator

/**
 * 인기도 점수 계산 유틸리티
 *
 * 중앙화된 인기도 점수 계산 로직을 제공하여 일관성을 보장합니다.
 * 가중치 변경 시 이 클래스만 수정하면 모든 곳에 적용됩니다.
 */
object PopularityCalculator {

    // 인기도 점수 가중치 상수
    private const val SALES_WEIGHT = 0.4
    private const val VIEW_WEIGHT = 0.3
    private const val WISH_WEIGHT = 0.3

    /**
     * 인기도 점수 계산
     *
     * @param salesCount 판매량 (가중치 40%)
     * @param viewCount 조회수 (가중치 30%)
     * @param wishCount 찜 개수 (가중치 30%)
     * @return 계산된 인기도 점수
     */
    fun calculateScore(salesCount: Long, viewCount: Long, wishCount: Long): Double {
        require(salesCount >= 0) { "판매량은 0 이상이어야 합니다" }
        require(viewCount >= 0) { "조회수는 0 이상이어야 합니다" }
        require(wishCount >= 0) { "찜 개수는 0 이상이어야 합니다" }

        return salesCount * SALES_WEIGHT + viewCount * VIEW_WEIGHT + wishCount * WISH_WEIGHT
    }

    /**
     * 가중치 정보 조회 (디버깅/로깅용)
     */
    fun getWeights(): Triple<Double, Double, Double> {
        return Triple(SALES_WEIGHT, VIEW_WEIGHT, WISH_WEIGHT)
    }
}