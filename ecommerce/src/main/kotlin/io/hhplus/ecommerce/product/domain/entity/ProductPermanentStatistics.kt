package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.product.domain.calculator.PopularityCalculator

/**
 * 상품 영구 통계 도메인 엔티티
 *
 * 목적:
 * - 상품별 누적 통계 데이터 표현 (총 조회수, 총 판매량, 총 찜 수)
 * - 장기 분석 및 비즈니스 인사이트 도출용
 * - 구매 전환율, 상품 성과 분석 등에 활용
 *
 * 설계 원칙:
 * - data class + 가변 필드로 성능과 편의성 모두 확보
 * - 비즈니스 로직 캡슐화
 * - JPA 엔티티와 분리된 순수 도메인 객체
 */
data class ProductPermanentStatistics(
    val productId: Long,
    var totalViewCount: Long,
    var totalSalesCount: Long,
    var totalWishCount: Long
) {

    init {
        require(productId > 0) { "상품 ID는 양수여야 합니다" }
        require(totalViewCount >= 0) { "총 조회수는 0 이상이어야 합니다" }
        require(totalSalesCount >= 0) { "총 판매량은 0 이상이어야 합니다" }
        require(totalWishCount >= 0) { "총 찜 수는 0 이상이어야 합니다" }
    }

    /**
     * 인기도 점수 계산 (중앙화된 로직 사용)
     */
    fun calculatePopularityScore(): Double {
        return PopularityCalculator.calculateScore(totalSalesCount, totalViewCount, totalWishCount)
    }

    /**
     * 조회수 증분 (배치 처리 최적화)
     */
    fun addViewCount(count: Long) {
        require(count >= 0) { "증분할 조회수는 0 이상이어야 합니다" }
        this.totalViewCount += count
    }

    /**
     * 판매량 증분 (배치 처리 최적화)
     */
    fun addSalesCount(count: Long) {
        require(count >= 0) { "증분할 판매량은 0 이상이어야 합니다" }
        this.totalSalesCount += count
    }

    /**
     * 찜 수 변경 (음수 방지, 배치 처리 최적화)
     */
    fun updateWishCount(delta: Long) {
        this.totalWishCount = maxOf(0, this.totalWishCount + delta)
    }

    companion object {
        /**
         * 신규 상품 통계 생성
         */
        fun create(productId: Long): ProductPermanentStatistics {
            return ProductPermanentStatistics(
                productId = productId,
                totalViewCount = 0,
                totalSalesCount = 0,
                totalWishCount = 0
            )
        }

        /**
         * 기존 통계 복원
         */
        fun restore(
            productId: Long,
            totalViewCount: Long,
            totalSalesCount: Long,
            totalWishCount: Long
        ): ProductPermanentStatistics {
            return ProductPermanentStatistics(
                productId = productId,
                totalViewCount = totalViewCount,
                totalSalesCount = totalSalesCount,
                totalWishCount = totalWishCount
            )
        }
    }
}