package io.hhplus.ecommerce.common.outbox.dlq

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class OutboxEventDlqRepositoryImpl(
    private val jpaRepository: OutboxEventDlqJpaRepository
) : OutboxEventDlqRepository {

    override fun save(dlqEvent: OutboxEventDlq): OutboxEventDlq {
        return jpaRepository.save(dlqEvent)
    }

    override fun findById(id: Long): OutboxEventDlq? {
        return jpaRepository.findByIdOrNull(id)
    }

    override fun findUnresolvedEvents(): List<OutboxEventDlq> {
        return jpaRepository.findUnresolvedEvents()
    }

    override fun findByOriginalEventId(originalEventId: Long): OutboxEventDlq? {
        return jpaRepository.findByOriginalEventId(originalEventId)
    }

    override fun findByEventType(eventType: String): List<OutboxEventDlq> {
        return jpaRepository.findByEventType(eventType)
    }

    override fun countUnresolved(): Long {
        return jpaRepository.countUnresolved()
    }
}
