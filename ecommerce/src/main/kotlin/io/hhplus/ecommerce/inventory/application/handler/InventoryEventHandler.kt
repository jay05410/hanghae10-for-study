package io.hhplus.ecommerce.inventory.application.handler

import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.common.idempotency.IdempotencyService
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.common.outbox.payload.InventoryInsufficientPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderCancelledPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import io.hhplus.ecommerce.inventory.exception.InventoryException
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * 재고 이벤트 핸들러 (Saga Step 2)
 *
 * PaymentCompleted → 재고 차감
 * OrderCancelled → 재고 복구
 *
 * 멱등성 보장: 같은 orderId에 대해 한 번만 처리
 */
@Component
class InventoryEventHandler(
    private val inventoryDomainService: InventoryDomainService,
    private val idempotencyService: IdempotencyService,
    private val outboxEventService: OutboxEventService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val IDEMPOTENCY_TTL = Duration.ofDays(7)
    }

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

            // 멱등성 체크
            val idempotencyKey = RedisKeyNames.Inventory.deductedKey(payload.orderId)
            if (!idempotencyService.tryAcquire(idempotencyKey, IDEMPOTENCY_TTL)) {
                logger.debug("[InventoryEventHandler] 이미 처리된 재고 차감, 스킵: orderId=${payload.orderId}")
                return true
            }

            logger.info("[InventoryEventHandler] 재고 차감 시작: orderId=${payload.orderId}")

            try {
                payload.items.sortedBy { it.productId }.forEach { item ->
                    inventoryDomainService.deductStock(item.productId, item.quantity)
                }
                logger.info("[InventoryEventHandler] 재고 차감 완료: orderId=${payload.orderId}")
            } catch (e: InventoryException.InsufficientStock) {
                // 재고 부족 → 보상 이벤트 발행 (주문 취소 트리거)
                logger.warn("[InventoryEventHandler] 재고 부족: orderId=${payload.orderId}, productId=${e.productId}")
                publishInventoryInsufficientEvent(payload, e)
            }

            true
        } catch (e: Exception) {
            logger.error("[InventoryEventHandler] 재고 차감 실패: ${e.message}", e)
            false
        }
    }

    private fun publishInventoryInsufficientEvent(
        payload: PaymentCompletedPayload,
        exception: InventoryException.InsufficientStock
    ) {
        val insufficientPayload = InventoryInsufficientPayload(
            orderId = payload.orderId,
            userId = payload.userId,
            productId = exception.productId,
            requestedQuantity = exception.requestedQuantity,
            availableQuantity = exception.availableQuantity,
            reason = "재고 부족: 상품 ${exception.productId}번의 가용 재고(${exception.availableQuantity})가 요청 수량(${exception.requestedQuantity})보다 부족합니다."
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.INVENTORY_INSUFFICIENT,
            aggregateType = EventRegistry.AggregateTypes.INVENTORY,
            aggregateId = payload.orderId.toString(),
            payload = json.encodeToString(InventoryInsufficientPayload.serializer(), insufficientPayload)
        )

        logger.info("[InventoryEventHandler] 재고 부족 이벤트 발행: orderId=${payload.orderId}")
    }

    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCancelledPayload>(event.payload)

            // 멱등성 체크
            val idempotencyKey = RedisKeyNames.Inventory.restoredKey(payload.orderId)
            if (!idempotencyService.tryAcquire(idempotencyKey, IDEMPOTENCY_TTL)) {
                logger.debug("[InventoryEventHandler] 이미 처리된 재고 복구, 스킵: orderId=${payload.orderId}")
                return true
            }

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
