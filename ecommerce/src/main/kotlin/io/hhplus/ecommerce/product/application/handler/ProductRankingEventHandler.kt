package io.hhplus.ecommerce.product.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.product.application.port.out.ProductRankingPort
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 상품 판매 랭킹 이벤트 핸들러
 *
 * 역할:
 * - PAYMENT_COMPLETED 이벤트 수신 시 판매량 랭킹 업데이트
 * - 주문 아이템별로 상품 판매량을 Redis Sorted Set에 반영
 *
 * Saga 흐름:
 * PaymentCompleted → ProductRankingEventHandler → Redis ZINCRBY
 *
 * 특징:
 * - 랭킹 업데이트 실패해도 주문 프로세스에 영향 없음 (비핵심 기능)
 * - 실패 시 로깅만 수행하고 true 반환 (재시도 불필요)
 *
 * @see docs/WEEK07_RANKING_ASYNC_DESIGN_PLAN.md
 */
@Component
class ProductRankingEventHandler(
    private val productRankingPort: ProductRankingPort,
    private val objectMapper: ObjectMapper
) : EventHandler {

    private val logger = KotlinLogging.logger {}

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.PAYMENT_COMPLETED)
    }

    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.PAYMENT_COMPLETED -> handlePaymentCompleted(event)
            else -> {
                logger.warn("[ProductRankingEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                true // 비핵심 기능이므로 실패해도 true 반환
            }
        }
    }

    /**
     * 결제 완료 이벤트 처리 - 판매량 랭킹 업데이트
     *
     * Payload 구조:
     * {
     *   "orderId": 123,
     *   "userId": 456,
     *   "amount": 50000,
     *   "items": [
     *     {"productId": 1, "quantity": 2, "unitPrice": 10000},
     *     {"productId": 2, "quantity": 1, "unitPrice": 30000}
     *   ]
     * }
     */
    private fun handlePaymentCompleted(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()

            @Suppress("UNCHECKED_CAST")
            val items = payload["items"] as? List<Map<String, Any>>

            if (items.isNullOrEmpty()) {
                logger.warn("[ProductRankingEventHandler] 주문 아이템이 없음: orderId=$orderId")
                return true // 아이템이 없어도 성공 처리
            }

            logger.info("[ProductRankingEventHandler] 판매 랭킹 업데이트 시작: orderId=$orderId, itemCount=${items.size}")

            var updatedCount = 0
            items.forEach { item ->
                try {
                    val productId = (item["productId"] as Number).toLong()
                    val quantity = (item["quantity"] as Number).toInt()

                    // Redis Sorted Set에 판매량 반영 (ZINCRBY)
                    val newDailySales = productRankingPort.incrementSalesCount(productId, quantity)

                    logger.debug(
                        "[ProductRankingEventHandler] 상품 랭킹 업데이트: productId=$productId, " +
                            "quantity=$quantity, newDailySales=$newDailySales"
                    )
                    updatedCount++
                } catch (e: Exception) {
                    // 개별 상품 업데이트 실패는 무시하고 계속 진행
                    logger.warn(
                        "[ProductRankingEventHandler] 상품 랭킹 업데이트 실패: item=$item, error=${e.message}"
                    )
                }
            }

            logger.info(
                "[ProductRankingEventHandler] 판매 랭킹 업데이트 완료: orderId=$orderId, " +
                    "updated=$updatedCount/${items.size}"
            )
            true
        } catch (e: Exception) {
            // 전체 처리 실패해도 true 반환 (랭킹은 비핵심 기능)
            logger.error("[ProductRankingEventHandler] 판매 랭킹 업데이트 실패: ${e.message}", e)
            true
        }
    }
}
