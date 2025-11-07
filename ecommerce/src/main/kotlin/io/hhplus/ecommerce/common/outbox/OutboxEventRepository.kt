package io.hhplus.ecommerce.common.outbox

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
    fun findById(id: Long): OutboxEvent?
    fun findUnprocessedEvents(): List<OutboxEvent>
    fun findByAggregateTypeAndAggregateId(aggregateType: String, aggregateId: String): List<OutboxEvent>
    fun findByEventType(eventType: String): List<OutboxEvent>
    fun deleteById(id: Long)
}