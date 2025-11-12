package io.hhplus.ecommerce.common.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface OutboxEventJpaRepository : JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false ORDER BY o.createdAt ASC")
    fun findUnprocessedEventsWithLimit(limit: Int): List<OutboxEvent>

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = true AND o.processedAt < :cutoffDate")
    fun findProcessedEventsBefore(cutoffDate: LocalDateTime): List<OutboxEvent>

    fun findByAggregateTypeAndAggregateId(aggregateType: String, aggregateId: String): List<OutboxEvent>

    fun findByEventType(eventType: String): List<OutboxEvent>

    fun deleteByIdIn(ids: List<Long>)
}