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
        val savedEntity = if (outboxEvent.id == 0L) {
            outboxEvent.copy(id = idGenerator.getAndIncrement())
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
            .take(limit)
    }

    override fun findProcessedEventsBefore(cutoffDate: LocalDateTime): List<OutboxEvent> {
        return storage.values
            .filter { it.processed && it.processedAt?.isBefore(cutoffDate) == true }
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

    fun clear() {
        storage.clear()
        idGenerator.set(1)
        initializeSampleData()
    }

    private fun OutboxEvent.copy(
        id: Long = this.id,
        eventType: String = this.eventType,
        aggregateType: String = this.aggregateType,
        aggregateId: String = this.aggregateId,
        payload: String = this.payload,
        processed: Boolean = this.processed,
        processedAt: java.time.LocalDateTime? = this.processedAt,
        errorMessage: String? = this.errorMessage,
        retryCount: Int = this.retryCount
    ): OutboxEvent {
        return OutboxEvent(id, eventType, aggregateType, aggregateId, payload, processed, processedAt, errorMessage, retryCount)
    }
}