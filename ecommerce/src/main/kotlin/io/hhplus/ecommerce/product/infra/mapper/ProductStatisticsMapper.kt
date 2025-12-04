package io.hhplus.ecommerce.product.infra.mapper

import io.hhplus.ecommerce.product.domain.entity.ProductPermanentStatistics
import io.hhplus.ecommerce.product.infra.persistence.entity.ProductStatisticsJpaEntity
import org.springframework.stereotype.Component

/**
 * 상품 통계 엔티티 매퍼
 *
 * JPA 엔티티 ↔ 도메인 엔티티 변환 처리
 * - 인프라 계층의 JPA 엔티티와 도메인 객체 분리
 * - 데이터 변환 및 유효성 검증
 */
@Component
class ProductStatisticsMapper {

    /**
     * JPA 엔티티 → 도메인 엔티티 변환
     */
    fun toDomain(jpaEntity: ProductStatisticsJpaEntity): ProductPermanentStatistics {
        return ProductPermanentStatistics.restore(
            productId = jpaEntity.productId,
            totalViewCount = jpaEntity.totalViewCount,
            totalSalesCount = jpaEntity.totalSalesCount,
            totalWishCount = jpaEntity.totalWishCount
        )
    }

    /**
     * 도메인 엔티티 → JPA 엔티티 변환 (신규 생성)
     */
    fun toJpaEntity(domain: ProductPermanentStatistics): ProductStatisticsJpaEntity {
        return ProductStatisticsJpaEntity.create(domain.productId).apply {
            totalViewCount = domain.totalViewCount
            totalSalesCount = domain.totalSalesCount
            totalWishCount = domain.totalWishCount
        }
    }

    /**
     * 기존 JPA 엔티티에 도메인 데이터 반영 (업데이트)
     */
    fun updateJpaEntity(
        jpaEntity: ProductStatisticsJpaEntity,
        domain: ProductPermanentStatistics
    ): ProductStatisticsJpaEntity {
        jpaEntity.totalViewCount = domain.totalViewCount
        jpaEntity.totalSalesCount = domain.totalSalesCount
        jpaEntity.totalWishCount = domain.totalWishCount
        return jpaEntity
    }

    /**
     * JPA 엔티티 리스트 → 도메인 엔티티 리스트 변환
     */
    fun toDomainList(jpaEntities: List<ProductStatisticsJpaEntity>): List<ProductPermanentStatistics> {
        return jpaEntities.map { toDomain(it) }
    }
}