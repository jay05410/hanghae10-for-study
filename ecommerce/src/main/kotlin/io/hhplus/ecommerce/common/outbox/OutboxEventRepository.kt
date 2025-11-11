package io.hhplus.ecommerce.common.outbox

import java.time.LocalDateTime

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
    fun findById(id: Long): OutboxEvent?
    fun findUnprocessedEvents(limit: Int = 100): List<OutboxEvent>
    fun findProcessedEventsBefore(cutoffDate: LocalDateTime): List<OutboxEvent>
    fun findByAggregateTypeAndAggregateId(aggregateType: String, aggregateId: String): List<OutboxEvent>
    fun findByEventType(eventType: String): List<OutboxEvent>
    fun deleteById(id: Long)
}