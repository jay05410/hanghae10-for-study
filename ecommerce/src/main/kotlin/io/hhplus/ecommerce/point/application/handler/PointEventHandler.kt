package io.hhplus.ecommerce.point.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.OrderCancelledPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 이벤트 핸들러 (Saga Step)
 *
 * 포인트 관련 책임은 포인트 도메인이 가짐
 * 결제에서는 검증만 하고, 실제 차감/환불은 이 핸들러가 담당
 *
 * PaymentCompleted → 포인트 차감
 * OrderCancelled → 포인트 환불
 */
@Component
class PointEventHandler(
    private val pointDomainService: PointDomainService
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
                logger.warn("[PointEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                false
            }
        }
    }

    private fun handlePaymentCompleted(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<PaymentCompletedPayload>(event.payload)

            logger.info("[PointEventHandler] 포인트 차감 시작: userId=${payload.userId}, amount=${payload.amount}")

            pointDomainService.usePoint(payload.userId, PointAmount.of(payload.amount))

            logger.info("[PointEventHandler] 포인트 차감 완료: userId=${payload.userId}")
            true
        } catch (e: Exception) {
            logger.error("[PointEventHandler] 포인트 차감 실패: ${e.message}", e)
            false
        }
    }

    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCancelledPayload>(event.payload)

            logger.info("[PointEventHandler] 포인트 환불 시작: userId=${payload.userId}, amount=${payload.finalAmount}")

            pointDomainService.earnPoint(payload.userId, PointAmount.of(payload.finalAmount))

            logger.info("[PointEventHandler] 포인트 환불 완료: userId=${payload.userId}")
            true
        } catch (e: Exception) {
            logger.error("[PointEventHandler] 포인트 환불 실패: ${e.message}", e)
            false
        }
    }
}
