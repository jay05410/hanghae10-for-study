package io.hhplus.ecommerce.coupon.application.consumer

import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.common.outbox.payload.CouponIssueRequestPayload
import io.hhplus.ecommerce.coupon.application.CouponIssueHistoryService
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.common.sse.CouponIssuedNotification
import io.hhplus.ecommerce.common.sse.SseEmitterService
import io.hhplus.ecommerce.common.sse.SseEventType
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 Kafka Consumer
 *
 * 기존 CouponIssueWorker(500ms 폴링)를 대체하여 이벤트 기반 처리
 *
 * 장점:
 * - 폴링 없이 요청 있을 때만 처리 (리소스 효율)
 * - Kafka 파티션 기반 병렬 처리
 * - 재시도 및 DLQ 자동 관리
 */
@Component
class CouponIssueConsumer(
    private val couponDomainService: CouponDomainService,
    private val couponRepository: CouponRepository,
    private val couponIssueHistoryService: CouponIssueHistoryService,
    private val sseEmitterService: SseEmitterService
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    @KafkaListener(
        topics = [Topics.Queue.COUPON_ISSUE],
        groupId = "#{@kafkaProperties.consumer.couponGroupId}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun consume(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.debug(
                "[CouponIssueConsumer] 메시지 수신: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key()
            )

            val payload = json.decodeFromString<CouponIssueRequestPayload>(record.value())

            processCouponIssue(payload)

            acknowledgment.acknowledge()

            logger.info(
                "[CouponIssueConsumer] 쿠폰 발급 완료: couponId={}, userId={}",
                payload.couponId, payload.userId
            )
        } catch (e: Exception) {
            logger.error(
                "[CouponIssueConsumer] 쿠폰 발급 실패: offset={}, error={}",
                record.offset(), e.message, e
            )
            acknowledgment.acknowledge()
        }
    }

    private fun processCouponIssue(payload: CouponIssueRequestPayload) {
        val coupon = couponRepository.findById(payload.couponId)
            ?: run {
                logger.warn("[CouponIssueConsumer] 쿠폰을 찾을 수 없음: couponId={}", payload.couponId)
                return
            }

        val userCoupon = couponDomainService.issueCoupon(coupon, payload.userId)

        couponIssueHistoryService.recordIssue(
            couponId = coupon.id,
            userId = payload.userId,
            couponName = coupon.name
        )

        // SSE 알림 전송
        sseEmitterService.sendEvent(
            userId = payload.userId,
            eventType = SseEventType.COUPON_ISSUED,
            data = CouponIssuedNotification(
                couponId = coupon.id,
                couponName = coupon.name
            )
        )

        logger.debug(
            "[CouponIssueConsumer] 발급 처리 완료: userCouponId={}, couponId={}, userId={}",
            userCoupon.id, payload.couponId, payload.userId
        )
    }
}
