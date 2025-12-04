package io.hhplus.ecommerce.inventory.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
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
    private val inventoryDomainService: InventoryDomainService,
    private val objectMapper: ObjectMapper
) : EventHandler {

    private val logger = KotlinLogging.logger {}

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
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()

            @Suppress("UNCHECKED_CAST")
            val items = payload["items"] as List<Map<String, Any>>

            logger.info("[InventoryEventHandler] 재고 차감 시작: orderId=$orderId")

            items.sortedBy { (it["productId"] as Number).toLong() }.forEach { item ->
                val productId = (item["productId"] as Number).toLong()
                val quantity = (item["quantity"] as Number).toInt()
                inventoryDomainService.deductStock(productId, quantity)
            }

            logger.info("[InventoryEventHandler] 재고 차감 완료: orderId=$orderId")
            true
        } catch (e: Exception) {
            logger.error("[InventoryEventHandler] 재고 차감 실패: ${e.message}", e)
            false
        }
    }

    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()

            @Suppress("UNCHECKED_CAST")
            val items = payload["items"] as List<Map<String, Any>>

            logger.info("[InventoryEventHandler] 재고 복구 시작: orderId=$orderId")

            items.forEach { item ->
                val productId = (item["productId"] as Number).toLong()
                val quantity = (item["quantity"] as Number).toInt()
                inventoryDomainService.restockInventory(productId, quantity)
            }

            logger.info("[InventoryEventHandler] 재고 복구 완료: orderId=$orderId")
            true
        } catch (e: Exception) {
            logger.error("[InventoryEventHandler] 재고 복구 실패: ${e.message}", e)
            false
        }
    }
}
