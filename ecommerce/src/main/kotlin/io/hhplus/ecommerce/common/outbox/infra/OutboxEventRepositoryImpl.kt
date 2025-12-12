package io.hhplus.ecommerce.common.outbox.infra

import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class OutboxEventRepositoryImpl(
    private val jpaRepository: OutboxEventJpaRepository
) : OutboxEventRepository {

    override fun save(outboxEvent: OutboxEvent): OutboxEvent =
        jpaRepository.save(outboxEvent)

    override fun findById(id: Long): OutboxEvent? =
        jpaRepository.findById(id).orElse(null)

    override fun findUnprocessedEvents(limit: Int): List<OutboxEvent> {
        val pageable = PageRequest.of(0, limit)
        return jpaRepository.findAll(pageable).content
            .filter { !it.processed }
            .sortedBy { it.createdAt }
    }

    override fun findProcessedEventsBefore(cutoffDate: LocalDateTime): List<OutboxEvent> =
        jpaRepository.findProcessedEventsBefore(cutoffDate)

    override fun findByAggregateTypeAndAggregateId(aggregateType: String, aggregateId: String): List<OutboxEvent> =
        jpaRepository.findByAggregateTypeAndAggregateId(aggregateType, aggregateId)

    override fun findByEventType(eventType: String): List<OutboxEvent> =
        jpaRepository.findByEventType(eventType)

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteByIds(ids: List<Long>) {
        jpaRepository.deleteByIdIn(ids)
    }
}
