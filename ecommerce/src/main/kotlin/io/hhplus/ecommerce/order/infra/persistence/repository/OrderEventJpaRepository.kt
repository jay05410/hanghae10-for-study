package io.hhplus.ecommerce.order.infra.persistence.repository

import io.hhplus.ecommerce.order.infra.persistence.entity.OrderEventJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Order Event Store JPA Repository
 *
 * Event Sourcing을 위한 이벤트 저장소 리포지토리
 */
@Repository
interface OrderEventJpaRepository : JpaRepository<OrderEventJpaEntity, Long> {

    /**
     * 특정 Aggregate의 모든 이벤트 조회 (버전 순)
     */
    fun findByAggregateIdOrderByVersionAsc(aggregateId: Long): List<OrderEventJpaEntity>

    /**
     * 특정 Aggregate의 특정 버전 이후 이벤트 조회
     * (스냅샷 이후 이벤트만 조회할 때 사용)
     */
    fun findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
        aggregateId: Long,
        version: Int
    ): List<OrderEventJpaEntity>

    /**
     * 특정 Aggregate의 최신 버전 조회
     */
    @Query("SELECT MAX(e.version) FROM OrderEventJpaEntity e WHERE e.aggregateId = :aggregateId")
    fun findMaxVersionByAggregateId(aggregateId: Long): Int?

    /**
     * 특정 Aggregate의 최신 이벤트 조회
     */
    fun findTopByAggregateIdOrderByVersionDesc(aggregateId: Long): OrderEventJpaEntity?

    /**
     * 특정 Aggregate의 이벤트 개수 조회
     * (스냅샷 생성 시점 결정에 사용)
     */
    fun countByAggregateId(aggregateId: Long): Long

    /**
     * 특정 이벤트 타입의 이벤트 조회 (분석/디버깅용)
     */
    fun findByEventTypeOrderByOccurredAtDesc(eventType: String): List<OrderEventJpaEntity>

    /**
     * 특정 기간 내 이벤트 조회 (분석/디버깅용)
     */
    @Query("""
        SELECT e FROM OrderEventJpaEntity e
        WHERE e.occurredAt >= :from AND e.occurredAt <= :to
        ORDER BY e.occurredAt ASC
    """)
    fun findByOccurredAtBetween(
        from: java.time.Instant,
        to: java.time.Instant
    ): List<OrderEventJpaEntity>
}
