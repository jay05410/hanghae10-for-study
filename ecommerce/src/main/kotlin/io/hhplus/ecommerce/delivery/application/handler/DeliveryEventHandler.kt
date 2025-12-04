package io.hhplus.ecommerce.delivery.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.delivery.domain.service.DeliveryDomainService
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
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
    private val deliveryDomainService: DeliveryDomainService,
    private val objectMapper: ObjectMapper
) : EventHandler {

    private val logger = KotlinLogging.logger {}

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.PAYMENT_COMPLETED)
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()

            @Suppress("UNCHECKED_CAST")
            val deliveryAddressMap = payload["deliveryAddress"] as? Map<String, Any>

            if (deliveryAddressMap == null) {
                logger.debug("[DeliveryEventHandler] 배송지 정보 없음: orderId=$orderId")
                return true
            }

            logger.info("[DeliveryEventHandler] 배송 생성 시작: orderId=$orderId")

            val deliveryAddress = DeliveryAddress(
                recipientName = deliveryAddressMap["recipientName"] as? String ?: "",
                phone = deliveryAddressMap["phone"] as? String ?: "",
                zipCode = deliveryAddressMap["zipCode"] as? String ?: "",
                address = deliveryAddressMap["address"] as? String ?: "",
                addressDetail = deliveryAddressMap["addressDetail"] as? String,
                deliveryMessage = deliveryAddressMap["deliveryMessage"] as? String
            )
            val deliveryMemo = deliveryAddressMap["deliveryMessage"] as? String

            deliveryDomainService.createDelivery(orderId, deliveryAddress, deliveryMemo)

            logger.info("[DeliveryEventHandler] 배송 생성 완료: orderId=$orderId")
            true
        } catch (e: Exception) {
            logger.error("[DeliveryEventHandler] 배송 생성 실패: ${e.message}", e)
            false
        }
    }
}
