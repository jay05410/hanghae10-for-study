package io.hhplus.ecommerce.common.outbox

import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryOutboxEventRepository : OutboxEventRepository {
    private val storage = ConcurrentHashMap<Long, OutboxEvent>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        // Sample processed order event
        val orderEvent = OutboxEvent(
            id = idGenerator.getAndIncrement(),
            eventType = "ORDER_CREATED",
            aggregateType = "Order",
            aggregateId = "1",
            payload = """{"orderId": 1, "userId": 1, "totalAmount": 40000, "status": "COMPLETED"}""",
            processed = true,
            processedAt = now.minusHours(2),
            errorMessage = null,
            retryCount = 0
        )

        // Sample unprocessed payment event
        val paymentEvent = OutboxEvent(
            id = idGenerator.getAndIncrement(),
            eventType = "PAYMENT_COMPLETED",
            aggregateType = "Payment",
            aggregateId = "1",
            payload = """{"paymentId": 1, "orderId": 1, "amount": 40000, "method": "BALANCE"}""",
            processed = false,
            processedAt = null,
            errorMessage = null,
            retryCount = 0
        )

        // Sample failed event with retry
        val inventoryEvent = OutboxEvent(
            id = idGenerator.getAndIncrement(),
            eventType = "INVENTORY_DEDUCTED",
            aggregateType = "Inventory",
            aggregateId = "1",
            payload = """{"productId": 1, "quantity": 2, "previousQuantity": 500}""",
            processed = false,
            processedAt = null,
            errorMessage = "External service temporarily unavailable",
            retryCount = 2
        )

        storage[orderEvent.id] = orderEvent
        storage[paymentEvent.id] = paymentEvent
        storage[inventoryEvent.id] = inventoryEvent
    }

    override fun save(outboxEvent: OutboxEvent): OutboxEvent {
        // ID가 없는 신규 이벤트인 경우 ID 할당
        val savedEntity = if (outboxEvent.id == 0L) {
            OutboxEvent(
                id = idGenerator.getAndIncrement(),
                eventType = outboxEvent.eventType,
                aggregateType = outboxEvent.aggregateType,
                aggregateId = outboxEvent.aggregateId,
                payload = outboxEvent.payload,
                processed = outboxEvent.processed,
                processedAt = outboxEvent.processedAt,
                errorMessage = outboxEvent.errorMessage,
                retryCount = outboxEvent.retryCount
            )
        } else {
            outboxEvent
        }
        storage[savedEntity.id] = savedEntity
        return savedEntity
    }

    override fun findById(id: Long): OutboxEvent? {
        return storage[id]
    }

    override fun findUnprocessedEvents(limit: Int): List<OutboxEvent> {
        return storage.values
            .filter { !it.processed }
            .sortedBy { it.id }  // FIFO 순서 보장
            .take(limit)
    }

    override fun findProcessedEventsBefore(cutoffDate: LocalDateTime): List<OutboxEvent> {
        return storage.values
            .filter { it.processed && it.processedAt?.isBefore(cutoffDate) == true }
            .sortedBy { it.id }  // 일관된 순서 보장
    }

    override fun findByAggregateTypeAndAggregateId(aggregateType: String, aggregateId: String): List<OutboxEvent> {
        return storage.values.filter { it.aggregateType == aggregateType && it.aggregateId == aggregateId }
    }

    override fun findByEventType(eventType: String): List<OutboxEvent> {
        return storage.values.filter { it.eventType == eventType }
    }

    override fun deleteById(id: Long) {
        storage.remove(id)
    }

    override fun deleteByIds(ids: List<Long>) {
        ids.forEach { storage.remove(it) }
    }

    fun clear() {
        storage.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}