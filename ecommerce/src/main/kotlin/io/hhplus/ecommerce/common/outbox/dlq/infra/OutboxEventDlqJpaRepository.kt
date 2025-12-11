package io.hhplus.ecommerce.common.outbox.dlq.infra

import io.hhplus.ecommerce.common.outbox.dlq.OutboxEventDlq
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OutboxEventDlqJpaRepository : JpaRepository<OutboxEventDlq, Long> {

    @Query("SELECT d FROM OutboxEventDlq d WHERE d.resolved = false ORDER BY d.failedAt ASC")
    fun findUnresolvedEvents(): List<OutboxEventDlq>

    fun findByOriginalEventId(originalEventId: Long): OutboxEventDlq?

    fun findByEventType(eventType: String): List<OutboxEventDlq>

    @Query("SELECT COUNT(d) FROM OutboxEventDlq d WHERE d.resolved = false")
    fun countUnresolved(): Long
}
