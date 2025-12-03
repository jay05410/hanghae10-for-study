package io.hhplus.ecommerce.product.infra.persistence.repository

import io.hhplus.ecommerce.product.infra.persistence.entity.ProductStatisticsJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * 상품 영구 통계 JPA 레포지토리
 *
 * 목적: 영구 증분 데이터의 RDB 영속성 관리
 * - 총 조회수, 총 판매량, 총 찜 수 저장
 * - 장기 분석을 위한 히스토리 데이터 보관
 */
interface ProductStatisticsJpaRepository : JpaRepository<ProductStatisticsJpaEntity, Long> {

    /**
     * 상품 ID로 통계 조회
     */
    fun findByProductId(productId: Long): ProductStatisticsJpaEntity?

    /**
     * 영구 데이터 기준 인기 상품 조회
     * 중앙화된 인기도 계산 공식 사용 (PopularityCalculator와 일치)
     */
    @Query("""
        SELECT ps FROM ProductStatisticsJpaEntity ps
        ORDER BY (ps.totalSalesCount * 0.4 + ps.totalViewCount * 0.3 + ps.totalWishCount * 0.3) DESC
    """)
    fun findTopByPermanentPopularity(pageable: Pageable): List<ProductStatisticsJpaEntity>
}