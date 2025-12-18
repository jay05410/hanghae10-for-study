package io.hhplus.ecommerce.delivery.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
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
 * PaymentCompleted → 배송 생성
 */
@Component
class DeliveryEventHandler(
    private val deliveryDomainService: DeliveryDomainService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.PAYMENT_COMPLETED)
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<PaymentCompletedPayload>(event.payload)

            if (payload.deliveryAddress == null) {
                logger.debug("[DeliveryEventHandler] 배송지 정보 없음: orderId=${payload.orderId}")
                return true
            }

            logger.info("[DeliveryEventHandler] 배송 생성 시작: orderId=${payload.orderId}")

            val deliveryMemo = payload.deliveryAddress.deliveryMessage
            deliveryDomainService.createDelivery(payload.orderId, payload.deliveryAddress, deliveryMemo)

            logger.info("[DeliveryEventHandler] 배송 생성 완료: orderId=${payload.orderId}")
            true
        } catch (e: Exception) {
            logger.error("[DeliveryEventHandler] 배송 생성 실패: ${e.message}", e)
            false
        }
    }
}
