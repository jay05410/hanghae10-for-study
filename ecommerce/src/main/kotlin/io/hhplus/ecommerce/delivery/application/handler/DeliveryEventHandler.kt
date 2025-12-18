package io.hhplus.ecommerce.delivery.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.OrderCreatedPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.delivery.domain.service.DeliveryDomainService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 배송 이벤트 핸들러 (Saga Step)
 *
 * 역할 분리:
 * - OrderCreated → 배송지 정보로 PENDING 상태 배송 생성
 * - PaymentCompleted → 배송 상태 PREPARING으로 전환
 *
 * 도메인 분리:
 * - Payment 이벤트에는 결제 정보만 (orderId, amount)
 * - 배송지 정보는 Order 이벤트에서 받음
 */
@Component
class DeliveryEventHandler(
    private val deliveryDomainService: DeliveryDomainService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(
            EventRegistry.EventTypes.ORDER_CREATED,
            EventRegistry.EventTypes.PAYMENT_COMPLETED
        )
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.ORDER_CREATED -> handleOrderCreated(event)
            EventRegistry.EventTypes.PAYMENT_COMPLETED -> handlePaymentCompleted(event)
            else -> {
                logger.warn("[DeliveryEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                true
            }
        }
    }

    /**
     * 주문 생성 시 배송 레코드 생성 (PENDING 상태)
     */
    private fun handleOrderCreated(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCreatedPayload>(event.payload)

            logger.info("[DeliveryEventHandler] 배송 생성 시작: orderId=${payload.orderId}")

            val deliveryMemo = payload.deliveryAddress.deliveryMessage
            deliveryDomainService.createDelivery(
                payload.orderId,
                payload.deliveryAddress,
                deliveryMemo
            )

            logger.info("[DeliveryEventHandler] 배송 생성 완료 (PENDING): orderId=${payload.orderId}")
            true
        } catch (e: Exception) {
            logger.error("[DeliveryEventHandler] 배송 생성 실패: ${e.message}", e)
            false
        }
    }

    /**
     * 결제 완료 시 배송 상태 PREPARING으로 전환
     */
    private fun handlePaymentCompleted(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<PaymentCompletedPayload>(event.payload)

            logger.info("[DeliveryEventHandler] 배송 준비 시작: orderId=${payload.orderId}")

            deliveryDomainService.startPreparing(payload.orderId)

            logger.info("[DeliveryEventHandler] 배송 상태 변경 완료 (PREPARING): orderId=${payload.orderId}")
            true
        } catch (e: Exception) {
            // 배송 준비 상태 전환 실패는 치명적이지 않음
            logger.warn("[DeliveryEventHandler] 배송 상태 전환 실패: ${e.message}")
            true
        }
    }
}
