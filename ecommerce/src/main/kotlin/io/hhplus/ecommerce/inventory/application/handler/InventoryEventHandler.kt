package io.hhplus.ecommerce.inventory.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.OrderCancelledPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 재고 이벤트 핸들러 (Saga Step 2)
 *
 * PaymentCompleted → 재고 차감
 * OrderCancelled → 재고 복구
 */
@Component
class InventoryEventHandler(
    private val inventoryDomainService: InventoryDomainService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(
            EventRegistry.EventTypes.PAYMENT_COMPLETED,
            EventRegistry.EventTypes.ORDER_CANCELLED
        )
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.PAYMENT_COMPLETED -> handlePaymentCompleted(event)
            EventRegistry.EventTypes.ORDER_CANCELLED -> handleOrderCancelled(event)
            else -> {
                logger.warn("[InventoryEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                false
            }
        }
    }

    private fun handlePaymentCompleted(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<PaymentCompletedPayload>(event.payload)

            logger.info("[InventoryEventHandler] 재고 차감 시작: orderId=${payload.orderId}")

            payload.items.sortedBy { it.productId }.forEach { item ->
                inventoryDomainService.deductStock(item.productId, item.quantity)
            }

            logger.info("[InventoryEventHandler] 재고 차감 완료: orderId=${payload.orderId}")
            true
        } catch (e: Exception) {
            logger.error("[InventoryEventHandler] 재고 차감 실패: ${e.message}", e)
            false
        }
    }

    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCancelledPayload>(event.payload)

            logger.info("[InventoryEventHandler] 재고 복구 시작: orderId=${payload.orderId}")

            payload.items.forEach { item ->
                inventoryDomainService.restockInventory(item.productId, item.quantity)
            }

            logger.info("[InventoryEventHandler] 재고 복구 완료: orderId=${payload.orderId}")
            true
        } catch (e: Exception) {
            logger.error("[InventoryEventHandler] 재고 복구 실패: ${e.message}", e)
            false
        }
    }
}
