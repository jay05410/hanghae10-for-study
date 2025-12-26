package io.hhplus.ecommerce.coupon.application.handler

import io.hhplus.ecommerce.common.messaging.KafkaMessagePublisher
import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.CouponIssueRequestPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderCancelledPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderConfirmedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 이벤트 핸들러 (Saga Step)
 *
 * 역할:
 * - OrderConfirmed → 쿠폰 상태 USED 확정
 * - OrderCancelled → 쿠폰 복구 (롤백)
 * - CouponIssueRequest → Kafka로 발급 요청 발행
 *
 * 쿠폰 검증 흐름:
 * - 쿠폰 적용 버튼 → 할인 미리보기만 (잠금 X)
 * - 결제 시점 → 쿠폰 유효성 + 미사용 검증
 * - 주문 확정 → USED 확정
 * - 주문 취소 → 쿠폰 복구
 *
 * 선착순 쿠폰 발급 흐름:
 * - CouponIssueService → Outbox → (이 핸들러) → Kafka → CouponIssueConsumer
 *
 * 주의:
 * - 할인 금액 계산은 주문 생성 시 PricingDomainService에서 이미 수행됨
 * - 이 핸들러에서는 쿠폰 상태만 변경 (재계산 없음)
 */
@Component
class CouponEventHandler(
    private val couponDomainService: CouponDomainService,
    private val kafkaMessagePublisher: KafkaMessagePublisher
) : EventHandler {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override fun supportedEventTypes(): List<String> {
        return listOf(
            EventRegistry.EventTypes.ORDER_CONFIRMED,
            EventRegistry.EventTypes.ORDER_CANCELLED,
            EventRegistry.EventTypes.COUPON_ISSUE_REQUEST
        )
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return when (event.eventType) {
            EventRegistry.EventTypes.ORDER_CONFIRMED -> handleOrderConfirmed(event)
            EventRegistry.EventTypes.ORDER_CANCELLED -> handleOrderCancelled(event)
            EventRegistry.EventTypes.COUPON_ISSUE_REQUEST -> handleCouponIssueRequest(event)
            else -> {
                logger.warn("[CouponEventHandler] 지원하지 않는 이벤트: ${event.eventType}")
                true
            }
        }
    }

    /**
     * 주문 확정 시 쿠폰 USED 확정 (다중 쿠폰 지원)
     */
    private fun handleOrderConfirmed(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderConfirmedPayload>(event.payload)

            if (payload.usedCouponIds.isEmpty()) {
                logger.debug("[CouponEventHandler] 사용된 쿠폰 없음: orderId=${payload.orderId}")
                return true
            }

            logger.info("[CouponEventHandler] 쿠폰 USED 확정 시작: orderId=${payload.orderId}, coupons=${payload.usedCouponIds.size}")

            couponDomainService.markCouponsAsUsed(
                userId = payload.userId,
                userCouponIds = payload.usedCouponIds,
                orderId = payload.orderId
            )

            logger.info("[CouponEventHandler] 쿠폰 USED 확정 완료: coupons=${payload.usedCouponIds}")
            true
        } catch (e: Exception) {
            logger.error("[CouponEventHandler] 쿠폰 상태 변경 실패: ${e.message}", e)
            true
        }
    }

    /**
     * 주문 취소 시 쿠폰 복구 (다중 쿠폰 지원)
     */
    private fun handleOrderCancelled(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<OrderCancelledPayload>(event.payload)

            if (payload.usedCouponIds.isEmpty()) {
                logger.debug("[CouponEventHandler] 복구할 쿠폰 없음: orderId=${payload.orderId}")
                return true
            }

            logger.info("[CouponEventHandler] 쿠폰 복구 시작: orderId=${payload.orderId}, coupons=${payload.usedCouponIds.size}")

            couponDomainService.releaseCoupons(
                userId = payload.userId,
                userCouponIds = payload.usedCouponIds
            )

            logger.info("[CouponEventHandler] 쿠폰 복구 완료: coupons=${payload.usedCouponIds}")
            true
        } catch (e: Exception) {
            logger.error("[CouponEventHandler] 쿠폰 복구 실패: ${e.message}", e)
            true
        }
    }

    /**
     * 선착순 쿠폰 발급 요청을 Kafka로 발행
     * CouponIssueConsumer가 Kafka에서 수신하여 실제 발급 처리
     */
    private fun handleCouponIssueRequest(event: OutboxEvent): Boolean {
        return try {
            val payload = json.decodeFromString<CouponIssueRequestPayload>(event.payload)

            kafkaMessagePublisher.publish(
                topic = Topics.COUPON,
                key = payload.couponId.toString(),
                payload = mapOf(
                    "couponId" to payload.couponId,
                    "userId" to payload.userId,
                    "couponName" to payload.couponName,
                    "requestedAt" to payload.requestedAt
                )
            )

            logger.info(
                "[CouponEventHandler] Kafka 발행 완료: couponId={}, userId={}",
                payload.couponId, payload.userId
            )

            true
        } catch (e: Exception) {
            logger.error(
                "[CouponEventHandler] 쿠폰 발급 요청 처리 실패: eventId={}, error={}",
                event.id, e.message, e
            )
            false
        }
    }
}
