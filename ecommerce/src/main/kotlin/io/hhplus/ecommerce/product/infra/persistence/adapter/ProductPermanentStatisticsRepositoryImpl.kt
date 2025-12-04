package io.hhplus.ecommerce.product.infra.persistence.adapter

import io.hhplus.ecommerce.product.domain.entity.ProductPermanentStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductPermanentStatisticsRepository
import io.hhplus.ecommerce.product.infra.persistence.mapper.ProductStatisticsMapper
import io.hhplus.ecommerce.product.infra.persistence.repository.ProductStatisticsJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

/**
 * 상품 영구 통계 레포지토리 구현체 - 인프라 계층 (어댑터)
 *
 * 책임:
 * - JPA 엔티티 ↔ 도메인 엔티티 매핑
 * - 영구 증분 데이터의 RDB 영속성 관리
 * - 매퍼를 통한 계층 분리 유지
 */
@Repository
class ProductPermanentStatisticsRepositoryImpl(
    private val productStatisticsJpaRepository: ProductStatisticsJpaRepository,
    private val statisticsMapper: ProductStatisticsMapper
) : ProductPermanentStatisticsRepository {

    override fun findByProductId(productId: Long): ProductPermanentStatistics? {
        return productStatisticsJpaRepository.findByProductId(productId)
            ?.let { statisticsMapper.toDomain(it) }
    }

    override fun save(statistics: ProductPermanentStatistics): ProductPermanentStatistics {
        // 기존 JPA 엔티티 조회 또는 신규 생성
        val existingJpaEntity = productStatisticsJpaRepository.findByProductId(statistics.productId)

        val jpaEntity = if (existingJpaEntity != null) {
            // 기존 엔티티 업데이트 (Version 유지)
            statisticsMapper.updateJpaEntity(existingJpaEntity, statistics)
        } else {
            // 신규 엔티티 생성
            statisticsMapper.toJpaEntity(statistics)
        }

        val savedJpaEntity = productStatisticsJpaRepository.save(jpaEntity)
        return statisticsMapper.toDomain(savedJpaEntity)
    }

    override fun findTopProductsByPermanentPopularity(limit: Int): List<ProductPermanentStatistics> {
        val pageable = PageRequest.of(0, limit)
        val jpaEntities = productStatisticsJpaRepository.findTopByPermanentPopularity(pageable)
        return statisticsMapper.toDomainList(jpaEntities)
    }
}
