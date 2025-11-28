package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 상품 통계 엔티티 - "자주 변경되는 데이터" 분리용
 *
 * 목적:
 * - Product 테이블과 워크로드 분리 (락 경합 감소)
 * - 이벤트 기반 영구 저장 (조회/판매/찜 총 누적값)
 * - 단방향 흐름: 이벤트 수집 → 청크 벌크 업데이트
 */
@Entity
@Table(name = "product_statistics")
class ProductStatistics(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val productId: Long,

    @Column(nullable = false)
    var totalViewCount: Long = 0,

    @Column(nullable = false)
    var totalSalesCount: Long = 0,

    @Column(nullable = false)
    var totalWishCount: Long = 0,

    @Version
    var version: Int = 0
) : BaseJpaEntity() {

    /**
     * 조회수 누적 (이벤트 벌크 업데이트용)
     */
    fun addViewCount(count: Long) {
        require(count >= 0) { "추가할 조회수는 0 이상이어야 합니다" }
        this.totalViewCount += count
    }

    /**
     * 판매량 누적 (이벤트 벌크 업데이트용)
     */
    fun addSalesCount(count: Long) {
        require(count >= 0) { "추가할 판매량은 0 이상이어야 합니다" }
        this.totalSalesCount += count
    }

    /**
     * 찜 개수 누적 (이벤트 벌크 업데이트용)
     */
    fun addWishCount(count: Long) {
        require(count >= 0) { "추가할 찜 개수는 0 이상이어야 합니다" }
        this.totalWishCount += count
    }

    /**
     * 인기도 점수 계산
     * 공식: 판매량 40% + 조회수 30% + 찜 30%
     */
    fun getPopularityScore(): Double {
        return totalSalesCount * 0.4 + totalViewCount * 0.3 + totalWishCount * 0.3
    }

    companion object {
        fun create(productId: Long): ProductStatistics {
            require(productId > 0) { "상품 ID는 양수여야 합니다" }

            return ProductStatistics(
                productId = productId
            )
        }
    }
}