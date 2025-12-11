package io.hhplus.ecommerce.coupon.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
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
    private val couponDomainService: CouponDomainService,
    private val objectMapper: ObjectMapper
) : EventHandler {

    private val logger = KotlinLogging.logger {}

    override fun supportedEventTypes(): List<String> {
        return listOf(EventRegistry.EventTypes.PAYMENT_COMPLETED)
    }

    @Transactional
    override fun handle(event: OutboxEvent): Boolean {
        return try {
            val payload = objectMapper.readValue(event.payload, Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()
            val userId = (payload["userId"] as Number).toLong()
            val amount = (payload["amount"] as Number).toLong()
            val usedCouponId = payload["usedCouponId"]?.let { (it as Number).toLong() }

            if (usedCouponId == null) {
                logger.debug("[CouponEventHandler] 사용된 쿠폰 없음: orderId=$orderId")
                return true
            }

            logger.info("[CouponEventHandler] 쿠폰 사용 처리 시작: orderId=$orderId, couponId=$usedCouponId")

            couponDomainService.applyCoupon(userId, usedCouponId, orderId, amount)

            logger.info("[CouponEventHandler] 쿠폰 사용 처리 완료: couponId=$usedCouponId")
            true
        } catch (e: Exception) {
            logger.error("[CouponEventHandler] 쿠폰 사용 처리 실패: ${e.message}", e)
            // 쿠폰 실패는 치명적이지 않으므로 true 반환
            true
        }
    }
}
