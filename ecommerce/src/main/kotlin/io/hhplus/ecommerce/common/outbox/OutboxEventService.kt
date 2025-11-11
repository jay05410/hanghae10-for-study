package io.hhplus.ecommerce.common.outbox

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OutboxEventService(
    private val outboxEventRepository: OutboxEventRepository,
    @Value("\${outbox.max-retry-count:5}")
    private val maxRetryCount: Int = 5
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

    fun getUnprocessedEvents(limit: Int = 100): List<OutboxEvent> {
        return outboxEventRepository.findUnprocessedEvents(limit)
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
        event.incrementRetryCount(maxRetryCount)
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

        if (event.retryCount >= maxRetryCount) {
            throw IllegalStateException("최대 재시도 횟수를 초과했습니다: $eventId")
        }

        // 재시도 준비: 에러 상태만 초기화 (재시도 횟수는 markAsFailed에서 이미 증가됨)
        event.errorMessage = null
        event.processedAt = null
        return outboxEventRepository.save(event)
    }

    @Transactional
    fun deleteProcessedEvents(olderThanDays: Long = 30) {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays)
        outboxEventRepository.findProcessedEventsBefore(cutoffDate)
            .forEach { outboxEventRepository.deleteById(it.id) }
    }
}