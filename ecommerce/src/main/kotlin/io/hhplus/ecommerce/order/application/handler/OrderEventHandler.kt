package io.hhplus.ecommerce.order.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentFailedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 주문 이벤트 핸들러
 *
 * 결제 관련 이벤트를 구독하여 주문 상태를 변경
 *
 * 이벤트 흐름:
 * - PaymentCompleted → 주문 상태 CONFIRMED로 변경
 * - PaymentFailed → 주문 상태 FAILED로 변경
 */
@Component
@Order(1)  // 가장 먼저 실행 - 주문 상태 변경 후 다른 핸들러 실행
class OrderEventHandler(
    private val orderDomainService: OrderDomainService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(
            EventRegistry.EventTypes.PAYMENT_COMPLETED,
            EventRegistry.EventTypes.PAYMENT_FAILED
        )
    }

    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.PAYMENT_COMPLETED -> confirmOrder(event)
            EventRegistry.EventTypes.PAYMENT_FAILED -> failOrder(event)
            else -> false
        }
    }

    /**
     * 결제 완료 시 주문 확정
     */
    private fun confirmOrder(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<PaymentCompletedPayload>(event.payload)

            val order = orderDomainService.getOrderOrThrow(payload.orderId)

            // 이미 CONFIRMED 상태면 중복 처리 방지
            if (order.status == OrderStatus.CONFIRMED) {
                logger.debug("[OrderEventHandler] 이미 확정된 주문: orderId={}", payload.orderId)
                return true
            }

            orderDomainService.confirmOrder(payload.orderId)
            logger.info("[OrderEventHandler] 주문 확정 완료: orderId={}", payload.orderId)

            true
        } catch (e: Exception) {
            logger.error("[OrderEventHandler] 주문 확정 실패: eventId={}, error={}", event.id, e.message, e)
            false
        }
    }

    /**
     * 결제 실패 시 주문 실패 처리
     */
    private fun failOrder(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<PaymentFailedPayload>(event.payload)

            val order = orderDomainService.getOrderOrThrow(payload.orderId)
            order.fail()
            orderDomainService.updateOrder(order)

            logger.info("[OrderEventHandler] 주문 실패 처리 완료: orderId={}, reason={}", payload.orderId, payload.reason)

            true
        } catch (e: Exception) {
            logger.error("[OrderEventHandler] 주문 실패 처리 실패: eventId={}, error={}", event.id, e.message, e)
            false
        }
    }
}
