package io.hhplus.ecommerce.common.outbox

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OutboxEventService(
    private val outboxEventRepository: OutboxEventRepository
) {

    @Transactional
    fun publishEvent(
        eventType: String,
        aggregateType: String,
        aggregateId: String,
        payload: String,
        createdBy: Long
    ): OutboxEvent {
        val outboxEvent = OutboxEvent.create(
            eventType = eventType,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payload = payload,
            createdBy = createdBy
        )

        return outboxEventRepository.save(outboxEvent)
    }

    fun getUnprocessedEvents(): List<OutboxEvent> {
        return outboxEventRepository.findUnprocessedEvents()
    }

    @Transactional
    fun markAsProcessed(eventId: Long): OutboxEvent? {
        val event = outboxEventRepository.findById(eventId)
            ?: return null

        event.markAsProcessed()
        return outboxEventRepository.save(event)
    }

    @Transactional
    fun markAsFailed(eventId: Long, errorMessage: String): OutboxEvent? {
        val event = outboxEventRepository.findById(eventId)
            ?: return null

        event.markAsFailed(errorMessage)
        event.incrementRetryCount()
        return outboxEventRepository.save(event)
    }

    fun getEventsByAggregate(aggregateType: String, aggregateId: String): List<OutboxEvent> {
        return outboxEventRepository.findByAggregateTypeAndAggregateId(aggregateType, aggregateId)
    }

    fun getEventsByType(eventType: String): List<OutboxEvent> {
        return outboxEventRepository.findByEventType(eventType)
    }

    @Transactional
    fun retryFailedEvent(eventId: Long): OutboxEvent? {
        val event = outboxEventRepository.findById(eventId)
            ?: return null

        if (event.retryCount >= 5) {
            throw IllegalStateException("최대 재시도 횟수를 초과했습니다: $eventId")
        }

        event.incrementRetryCount()
        return outboxEventRepository.save(event)
    }

    @Transactional
    fun deleteProcessedEvents(olderThanDays: Long = 30) {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays)
        outboxEventRepository.findUnprocessedEvents()
            .filter { it.processed && it.processedAt?.isBefore(cutoffDate) == true }
            .forEach { outboxEventRepository.deleteById(it.id) }
    }
}