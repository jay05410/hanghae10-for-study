package io.hhplus.ecommerce.order.infra.persistence.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.hhplus.ecommerce.order.domain.eventsourcing.*
import io.hhplus.ecommerce.order.infra.persistence.entity.OrderEventJpaEntity
import io.hhplus.ecommerce.order.infra.persistence.repository.OrderEventJpaRepository
import org.springframework.stereotype.Repository

/**
 * Order Event Repository 구현체
 *
 * Event Sourcing을 위한 이벤트 저장소 구현
 * - JSON 직렬화/역직렬화
 * - 낙관적 잠금 처리
 */
@Repository
class OrderEventRepositoryImpl(
    private val orderEventJpaRepository: OrderEventJpaRepository,
    private val objectMapper: ObjectMapper
) : OrderEventRepository {

    override fun save(event: OrderEvent, expectedVersion: Int): OrderEventRecord {
        // 낙관적 잠금: 현재 버전 확인
        val currentVersion = getCurrentVersion(event.orderId)
        if (currentVersion != expectedVersion) {
            throw OptimisticLockException(
                "버전 충돌: expected=$expectedVersion, current=$currentVersion, aggregateId=${event.orderId}"
            )
        }

        val newVersion = expectedVersion + 1
        val entity = OrderEventJpaEntity(
            aggregateId = event.orderId,
            eventType = event.eventType,
            payload = objectMapper.writeValueAsString(event),
            version = newVersion,
            occurredAt = event.occurredAt
        )

        val saved = orderEventJpaRepository.save(entity)

        return OrderEventRecord(
            id = saved.id,
            aggregateId = saved.aggregateId,
            eventType = saved.eventType,
            version = saved.version,
            event = event
        )
    }

    override fun saveAll(
        events: List<OrderEvent>,
        aggregateId: Long,
        expectedVersion: Int
    ): List<OrderEventRecord> {
        if (events.isEmpty()) return emptyList()

        // 낙관적 잠금: 현재 버전 확인
        val currentVersion = getCurrentVersion(aggregateId)
        if (currentVersion != expectedVersion) {
            throw OptimisticLockException(
                "버전 충돌: expected=$expectedVersion, current=$currentVersion, aggregateId=$aggregateId"
            )
        }

        val records = mutableListOf<OrderEventRecord>()
        var version = expectedVersion

        events.forEach { event ->
            version++
            val entity = OrderEventJpaEntity(
                aggregateId = aggregateId,
                eventType = event.eventType,
                payload = objectMapper.writeValueAsString(event),
                version = version,
                occurredAt = event.occurredAt
            )
            val saved = orderEventJpaRepository.save(entity)

            records.add(
                OrderEventRecord(
                    id = saved.id,
                    aggregateId = saved.aggregateId,
                    eventType = saved.eventType,
                    version = saved.version,
                    event = event
                )
            )
        }

        return records
    }

    override fun findByAggregateId(aggregateId: Long): List<OrderEvent> {
        return orderEventJpaRepository
            .findByAggregateIdOrderByVersionAsc(aggregateId)
            .map { deserialize(it) }
    }

    override fun findByAggregateIdAfterVersion(aggregateId: Long, afterVersion: Int): List<OrderEvent> {
        return orderEventJpaRepository
            .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, afterVersion)
            .map { deserialize(it) }
    }

    override fun getCurrentVersion(aggregateId: Long): Int {
        return orderEventJpaRepository.findMaxVersionByAggregateId(aggregateId) ?: 0
    }

    override fun countByAggregateId(aggregateId: Long): Long {
        return orderEventJpaRepository.countByAggregateId(aggregateId)
    }

    override fun existsByAggregateId(aggregateId: Long): Boolean {
        return countByAggregateId(aggregateId) > 0
    }

    /**
     * JSON 역직렬화
     */
    private fun deserialize(entity: OrderEventJpaEntity): OrderEvent {
        return when (entity.eventType) {
            "OrderCreated" -> objectMapper.readValue<OrderEvent.OrderCreated>(entity.payload)
            "OrderConfirmed" -> objectMapper.readValue<OrderEvent.OrderConfirmed>(entity.payload)
            "OrderCompleted" -> objectMapper.readValue<OrderEvent.OrderCompleted>(entity.payload)
            "OrderCancelled" -> objectMapper.readValue<OrderEvent.OrderCancelled>(entity.payload)
            "OrderFailed" -> objectMapper.readValue<OrderEvent.OrderFailed>(entity.payload)
            else -> throw IllegalArgumentException("알 수 없는 이벤트 타입: ${entity.eventType}")
        }
    }
}

/**
 * 낙관적 잠금 예외
 */
class OptimisticLockException(message: String) : RuntimeException(message)
