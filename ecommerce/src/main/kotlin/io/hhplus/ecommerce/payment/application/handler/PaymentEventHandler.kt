package io.hhplus.ecommerce.payment.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.common.outbox.payload.OrderCreatedPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.common.outbox.payload.PaymentFailedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 결제 이벤트 핸들러 (Saga Step 1)
 *
 * OrderCreated 이벤트 수신 → 결제 처리 → PaymentCompleted/PaymentFailed 발행
 *
 * Saga 흐름:
 * OrderCreated → [PaymentEventHandler] → PaymentCompleted → 다음 핸들러들
 *                                      → PaymentFailed → 보상 트랜잭션
 */
@Component
class PaymentEventHandler(
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val outboxEventService: OutboxEventService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.ORDER_CREATED)
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCreatedPayload>(event.payload)

            logger.info("[PaymentEventHandler] 결제 처리 시작: orderId=${payload.orderId}, userId=${payload.userId}, amount=${payload.finalAmount}")

            try {
                // 결제 처리
                val payment = processPaymentUseCase.execute(
                    ProcessPaymentRequest(
                        userId = payload.userId,
                        orderId = payload.orderId,
                        amount = payload.finalAmount
                    )
                )

                // 결제 성공 이벤트 발행
                publishPaymentCompletedEvent(payload, payment.id)
                logger.info("[PaymentEventHandler] 결제 성공: paymentId=${payment.id}")

            } catch (e: Exception) {
                // 결제 실패 이벤트 발행
                publishPaymentFailedEvent(payload, e.message ?: "결제 실패")
                logger.error("[PaymentEventHandler] 결제 실패: ${e.message}")
            }

            true
        } catch (e: Exception) {
            logger.error("[PaymentEventHandler] 이벤트 처리 실패: ${e.message}", e)
            false
        }
    }

    private fun publishPaymentCompletedEvent(
        originalPayload: OrderCreatedPayload,
        paymentId: Long
    ) {
        val payload = PaymentCompletedPayload(
            orderId = originalPayload.orderId,
            userId = originalPayload.userId,
            paymentId = paymentId,
            amount = originalPayload.finalAmount,
            usedCouponId = originalPayload.usedCouponId,
            items = originalPayload.items,
            deliveryAddress = originalPayload.deliveryAddress
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.PAYMENT_COMPLETED,
            aggregateType = EventRegistry.AggregateTypes.PAYMENT,
            aggregateId = paymentId.toString(),
            payload = json.encodeToString(PaymentCompletedPayload.serializer(), payload)
        )
    }

    private fun publishPaymentFailedEvent(
        originalPayload: OrderCreatedPayload,
        reason: String
    ) {
        val payload = PaymentFailedPayload(
            orderId = originalPayload.orderId,
            userId = originalPayload.userId,
            reason = reason,
            items = originalPayload.items
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.PAYMENT_FAILED,
            aggregateType = EventRegistry.AggregateTypes.PAYMENT,
            aggregateId = originalPayload.orderId.toString(),
            payload = json.encodeToString(PaymentFailedPayload.serializer(), payload)
        )
    }
}
