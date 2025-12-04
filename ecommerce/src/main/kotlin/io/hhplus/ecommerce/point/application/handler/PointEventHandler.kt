package io.hhplus.ecommerce.point.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 이벤트 핸들러 (Saga Step)
 *
 * PaymentCompleted → 포인트 차감
 * OrderCancelled → 포인트 환불
 */
@Component
class PointEventHandler(
    private val pointDomainService: PointDomainService,
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
                logger.warn("[PointEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                false
            }
        }
    }

    private fun handlePaymentCompleted(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val amount = (payload["amount"] as Number).toLong()

            logger.info("[PointEventHandler] 포인트 차감 시작: userId=$userId, amount=$amount")

            pointDomainService.usePoint(userId, PointAmount.of(amount))

            logger.info("[PointEventHandler] 포인트 차감 완료: userId=$userId")
            true
        } catch (e: Exception) {
            logger.error("[PointEventHandler] 포인트 차감 실패: ${e.message}", e)
            false
        }
    }

    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val userId = (payload["userId"] as Number).toLong()
            val finalAmount = (payload["finalAmount"] as Number).toLong()

            logger.info("[PointEventHandler] 포인트 환불 시작: userId=$userId, amount=$finalAmount")

            pointDomainService.earnPoint(userId, PointAmount.of(finalAmount))

            logger.info("[PointEventHandler] 포인트 환불 완료: userId=$userId")
            true
        } catch (e: Exception) {
            logger.error("[PointEventHandler] 포인트 환불 실패: ${e.message}", e)
            false
        }
    }
}
