package io.hhplus.ecommerce.order.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * Order Event Store JPA 엔티티 (Event Sourcing)
 *
 * 역할:
 * - 주문 도메인 이벤트의 영속화
 * - Event Store 테이블 매핑
 * - 낙관적 잠금을 위한 version 관리
 *
 * 특징:
 * - 이벤트는 불변이므로 INSERT만 발생
 * - aggregateId + version 조합은 유니크
 * - payload는 JSON 형식으로 저장
 */
@Entity
@Table(
    name = "order_events",
    indexes = [
        Index(name = "idx_order_events_aggregate_id", columnList = "aggregateId"),
        Index(name = "idx_order_events_occurred_at", columnList = "occurredAt")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_order_events_aggregate_version",
            columnNames = ["aggregateId", "version"]
        )
    ]
)
class OrderEventJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Aggregate ID (Order ID)
     */
    @Column(nullable = false)
    val aggregateId: Long,

    /**
     * 이벤트 타입 (OrderCreated, OrderConfirmed, etc.)
     */
    @Column(nullable = false, length = 100)
    val eventType: String,

    /**
     * 이벤트 페이로드 (JSON)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    /**
     * Aggregate 버전 (낙관적 잠금)
     * 동일 aggregateId에서 순차적으로 증가
     */
    @Column(nullable = false)
    val version: Int,

    /**
     * 이벤트 발생 시각
     */
    @Column(nullable = false)
    val occurredAt: Instant,

    /**
     * 이벤트 메타데이터 (선택)
     * - traceId, correlationId, causationId 등
     */
    @Column(columnDefinition = "TEXT")
    val metadata: String? = null
) : BaseJpaEntity()
