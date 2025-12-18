package io.hhplus.ecommerce.cart.application.handler

import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.OrderCancelledPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderCreatedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 이벤트 핸들러 (Saga Step)
 *
 * 역할:
 * - OrderCreated → 주문된 상품 장바구니에서 제거
 * - OrderCancelled → 장바구니로 상품 복구 (롤백)
 *
 * 도메인 분리:
 * - Order 이벤트에서 items 정보 직접 사용 (Order DI 불필요)
 * - Cart 도메인은 자기 역할만 수행 (제거/복구)
 */
@Component
class CartEventHandler(
    private val cartDomainService: CartDomainService
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(
            EventRegistry.EventTypes.ORDER_CREATED,
            EventRegistry.EventTypes.ORDER_CANCELLED
        )
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.ORDER_CREATED -> handleOrderCreated(event)
            EventRegistry.EventTypes.ORDER_CANCELLED -> handleOrderCancelled(event)
            else -> {
                logger.warn("[CartEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                true
            }
        }
    }

    /**
     * 주문 생성 시 장바구니에서 상품 제거
     */
    private fun handleOrderCreated(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCreatedPayload>(event.payload)

            logger.info("[CartEventHandler] 장바구니 정리 시작: userId=${payload.userId}, orderId=${payload.orderId}")

            val orderedProductIds = payload.items.map { it.productId }
            if (orderedProductIds.isEmpty()) {
                logger.debug("[CartEventHandler] 주문 상품 없음: orderId=${payload.orderId}")
                return true
            }

            cartDomainService.removeOrderedItems(payload.userId, orderedProductIds)

            logger.info("[CartEventHandler] 장바구니 정리 완료: userId=${payload.userId}, 제거된 상품 수=${orderedProductIds.size}")
            true
        } catch (e: Exception) {
            logger.error("[CartEventHandler] 장바구니 정리 실패: ${e.message}", e)
            // 장바구니 정리 실패는 치명적이지 않으므로 true 반환
            true
        }
    }

    /**
     * 주문 취소 시 장바구니로 상품 복구
     */
    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCancelledPayload>(event.payload)

            logger.info("[CartEventHandler] 장바구니 복구 시작: userId=${payload.userId}, orderId=${payload.orderId}")

            if (payload.items.isEmpty()) {
                logger.debug("[CartEventHandler] 복구할 상품 없음: orderId=${payload.orderId}")
                return true
            }

            // 상품을 장바구니에 다시 추가
            payload.items.forEach { item ->
                try {
                    cartDomainService.addToCart(payload.userId, item.productId, item.quantity)
                } catch (e: Exception) {
                    logger.warn("[CartEventHandler] 상품 복구 실패: productId=${item.productId}, error=${e.message}")
                }
            }

            logger.info("[CartEventHandler] 장바구니 복구 완료: userId=${payload.userId}, 복구된 상품 수=${payload.items.size}")
            true
        } catch (e: Exception) {
            logger.error("[CartEventHandler] 장바구니 복구 실패: ${e.message}", e)
            // 장바구니 복구 실패는 치명적이지 않으므로 true 반환
            true
        }
    }
}
