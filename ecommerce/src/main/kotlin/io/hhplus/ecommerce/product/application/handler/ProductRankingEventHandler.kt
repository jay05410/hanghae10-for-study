package io.hhplus.ecommerce.product.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.OrderConfirmedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.product.application.port.out.ProductRankingPort
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 상품 랭킹 이벤트 핸들러
 *
 * OrderConfirmed 이벤트를 수신하여 Redis에 판매량 기록
 *
 * 흐름:
 * 1. OrderConfirmed 이벤트 수신
 * 2. 주문 상품별 판매량을 Redis Sorted Set에 기록
 * 3. 스케줄러가 주기적으로 Redis → DB 동기화
 *
 * Redis 자료구조:
 * - 일별: product:ranking:daily:{yyyyMMdd}
 * - 주별: product:ranking:weekly:{yyyyWW}
 * - 누적: product:ranking:total
 */
@Component
class ProductRankingEventHandler(
    private val productRankingPort: ProductRankingPort
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.ORDER_CONFIRMED)
    }

    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.ORDER_CONFIRMED -> handleOrderConfirmed(event)
            else -> {
                logger.warn("[ProductRankingEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                true
            }
        }
    }

    /**
     * 주문 확정 시 판매량 기록
     *
     * 배치 처리로 Redis RTT 최소화
     */
    private fun handleOrderConfirmed(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderConfirmedPayload>(event.payload)

            if (payload.items.isEmpty()) {
                logger.debug("[ProductRankingEventHandler] 주문 상품 없음: orderId=${payload.orderId}")
                return true
            }

            logger.info("[ProductRankingEventHandler] 판매량 기록 시작: orderId=${payload.orderId}, items=${payload.items.size}")

            // 상품별 판매량을 Map으로 변환하여 배치 처리
            val salesByProduct = payload.items.associate { it.productId to it.quantity }

            // 배치로 Redis에 기록 (Pipeline 최적화)
            productRankingPort.incrementSalesCountBatch(salesByProduct)

            logger.info("[ProductRankingEventHandler] 판매량 기록 완료: orderId=${payload.orderId}")
            true
        } catch (e: Exception) {
            logger.error("[ProductRankingEventHandler] 판매량 기록 실패: ${e.message}", e)
            // 판매량 기록 실패는 치명적이지 않음 - 재시도 후 실패해도 OK
            true
        }
    }
}
