package io.hhplus.ecommerce.product.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 상품 영구 통계 JPA 엔티티
 *
 * 목적: Product 테이블과 워크로드 분리 및 영구 증분 데이터 저장
 * - 총 조회수, 총 판매량, 총 찜 수 (누적값, 감소하지 않음)
 * - 장기 통계 분석, 구매 전환율 계산용
 * - 상품 성과 분석, 트렌드 분석용 기초 데이터
 *
 * 특징:
 * - 영속성 필요 (히스토리 분석용)
 * - 이벤트 기반 배치 업데이트 (락 경합 최소화)
 * - Product 테이블 락 경합 방지
 */
@Entity
@Table(
    name = "product_statistics",
    indexes = [
        Index(name = "idx_product_statistics_product_id", columnList = "productId"),
        Index(name = "idx_product_statistics_popularity", columnList = "totalSalesCount DESC, totalViewCount DESC")
    ]
)
class ProductStatisticsJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val productId: Long,

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    var totalViewCount: Long = 0,

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    var totalSalesCount: Long = 0,

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    var totalWishCount: Long = 0,

    @Version
    var version: Int = 0
) : BaseJpaEntity() {

    /**
     * 조회수 증분 (배치 업데이트용)
     */
    fun incrementViewCount(count: Long) {
        require(count >= 0) { "증분할 조회수는 0 이상이어야 합니다" }
        this.totalViewCount += count
    }

    /**
     * 판매량 증분 (배치 업데이트용)
     */
    fun incrementSalesCount(count: Long) {
        require(count >= 0) { "증분할 판매량은 0 이상이어야 합니다" }
        this.totalSalesCount += count
    }

    /**
     * 찜 수 변경 (증가/감소 모두 처리, 음수 방지)
     */
    fun updateWishCount(delta: Long) {
        this.totalWishCount = maxOf(0, this.totalWishCount + delta)
    }

    companion object {
        /**
         * 신규 상품 통계 생성
         */
        fun create(productId: Long): ProductStatisticsJpaEntity {
            require(productId > 0) { "상품 ID는 양수여야 합니다" }

            return ProductStatisticsJpaEntity(
                productId = productId
            )
        }
    }
}