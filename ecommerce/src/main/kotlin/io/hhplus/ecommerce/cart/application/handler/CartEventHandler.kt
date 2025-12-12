package io.hhplus.ecommerce.cart.application.handler

import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 이벤트 핸들러 (Saga Step)
 *
 * PaymentCompleted → 주문된 상품 장바구니에서 제거
 */
@Component
class CartEventHandler(
    private val cartDomainService: CartDomainService
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

            logger.info("[CartEventHandler] 장바구니 정리 시작: userId=${payload.userId}, orderId=${payload.orderId}")

            val orderedProductIds = payload.items.map { it.productId }
            cartDomainService.removeOrderedItems(payload.userId, orderedProductIds)

            logger.info("[CartEventHandler] 장바구니 정리 완료: userId=${payload.userId}, 제거된 상품 수=${orderedProductIds.size}")
            true
        } catch (e: Exception) {
            logger.error("[CartEventHandler] 장바구니 정리 실패: ${e.message}", e)
            // 장바구니 정리 실패는 치명적이지 않으므로 true 반환
            true
        }
    }
}
