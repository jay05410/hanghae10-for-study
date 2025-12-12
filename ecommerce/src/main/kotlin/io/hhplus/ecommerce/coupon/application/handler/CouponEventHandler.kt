package io.hhplus.ecommerce.coupon.application.handler

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.payload.PaymentCompletedPayload
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 이벤트 핸들러 (Saga Step)
 *
 * PaymentCompleted → 쿠폰 사용 처리
 */
@Component
class CouponEventHandler(
    private val couponDomainService: CouponDomainService
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

            if (payload.usedCouponId == null) {
                logger.debug("[CouponEventHandler] 사용된 쿠폰 없음: orderId=${payload.orderId}")
                return true
            }

            logger.info("[CouponEventHandler] 쿠폰 사용 처리 시작: orderId=${payload.orderId}, couponId=${payload.usedCouponId}")

            couponDomainService.applyCoupon(payload.userId, payload.usedCouponId, payload.orderId, payload.amount)

            logger.info("[CouponEventHandler] 쿠폰 사용 처리 완료: couponId=${payload.usedCouponId}")
            true
        } catch (e: Exception) {
            logger.error("[CouponEventHandler] 쿠폰 사용 처리 실패: ${e.message}", e)
            // 쿠폰 실패는 치명적이지 않으므로 true 반환
            true
        }
    }
}
