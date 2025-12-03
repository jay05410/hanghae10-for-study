package io.hhplus.ecommerce.product.domain.repository

import io.hhplus.ecommerce.product.domain.entity.ProductPermanentStatistics

/**
 * 상품 영구 통계 도메인 레포지토리
 *
 * 목적:
 * - 영구 증분 통계 데이터의 영속성 관리
 * - 도메인 엔티티 기반 인터페이스 제공
 * - 장기 분석을 위한 누적 데이터 저장
 *
 * 책임:
 * - 상품별 총 조회수, 총 판매량, 총 찜 수 관리
 * - 배치 업데이트를 통한 성능 최적화
 * - 비즈니스 로직과 무관한 순수 데이터 접근 계층
 */
interface ProductPermanentStatisticsRepository {

    /**
     * 상품 ID로 영구 통계 조회
     */
    fun findByProductId(productId: Long): ProductPermanentStatistics?

    /**
     * 영구 통계 저장/업데이트
     */
    fun save(statistics: ProductPermanentStatistics): ProductPermanentStatistics

    /**
     * 인기 상품 통계 조회 (영구 데이터 기준)
     * 장기 분석용으로 사용
     */
    fun findTopProductsByPermanentPopularity(limit: Int): List<ProductPermanentStatistics>
}