package io.hhplus.ecommerce.common.outbox.dlq

interface OutboxEventDlqRepository {
    fun save(dlqEvent: OutboxEventDlq): OutboxEventDlq
    fun findById(id: Long): OutboxEventDlq?
    fun findUnresolvedEvents(): List<OutboxEventDlq>
    fun findByOriginalEventId(originalEventId: Long): OutboxEventDlq?
    fun findByEventType(eventType: String): List<OutboxEventDlq>
    fun countUnresolved(): Long
}
