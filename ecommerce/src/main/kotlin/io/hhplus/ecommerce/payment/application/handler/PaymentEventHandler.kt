package io.hhplus.ecommerce.payment.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
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
    private val outboxEventService: OutboxEventService,
    private val objectMapper: ObjectMapper
) : EventHandler {

    private val logger = KotlinLogging.logger {}

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.ORDER_CREATED)
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)

            val orderId = (payload["orderId"] as Number).toLong()
            val userId = (payload["userId"] as Number).toLong()
            val finalAmount = (payload["finalAmount"] as Number).toLong()

            logger.info("[PaymentEventHandler] 결제 처리 시작: orderId=$orderId, userId=$userId, amount=$finalAmount")

            try {
                // 결제 처리
                val payment = processPaymentUseCase.execute(
                    ProcessPaymentRequest(
                        userId = userId,
                        orderId = orderId,
                        amount = finalAmount
                    )
                )

                // 결제 성공 이벤트 발행
                publishPaymentCompletedEvent(orderId, userId, payment.id, finalAmount, payload)
                logger.info("[PaymentEventHandler] 결제 성공: paymentId=${payment.id}")

            } catch (e: Exception) {
                // 결제 실패 이벤트 발행
                publishPaymentFailedEvent(orderId, userId, e.message ?: "결제 실패", payload)
                logger.error("[PaymentEventHandler] 결제 실패: ${e.message}")
            }

            true
        } catch (e: Exception) {
            logger.error("[PaymentEventHandler] 이벤트 처리 실패: ${e.message}", e)
            false
        }
    }

    private fun publishPaymentCompletedEvent(
        orderId: Long,
        userId: Long,
        paymentId: Long,
        amount: Long,
        originalPayload: Map<*, *>
    ) {
        val payload = mapOf(
            "orderId" to orderId,
            "userId" to userId,
            "paymentId" to paymentId,
            "amount" to amount,
            "usedCouponId" to originalPayload["usedCouponId"],
            "items" to originalPayload["items"],
            "deliveryAddress" to originalPayload["deliveryAddress"]
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.PAYMENT_COMPLETED,
            aggregateType = EventRegistry.AggregateTypes.PAYMENT,
            aggregateId = paymentId.toString(),
            payload = objectMapper.writeValueAsString(payload)
        )
    }

    private fun publishPaymentFailedEvent(
        orderId: Long,
        userId: Long,
        reason: String,
        originalPayload: Map<*, *>
    ) {
        val payload = mapOf(
            "orderId" to orderId,
            "userId" to userId,
            "reason" to reason,
            "items" to originalPayload["items"]
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.PAYMENT_FAILED,
            aggregateType = EventRegistry.AggregateTypes.PAYMENT,
            aggregateId = orderId.toString(),
            payload = objectMapper.writeValueAsString(payload)
        )
    }
}
