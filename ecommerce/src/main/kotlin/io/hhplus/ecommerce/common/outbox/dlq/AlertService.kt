package io.hhplus.ecommerce.common.outbox.dlq

/**
 * 알림 서비스 인터페이스
 *
 * DLQ 이벤트 발생 시 운영자에게 알림 전송
 * 구현체에 따라 Slack, Email, SMS 등 다양한 채널 지원 가능
 */
interface AlertService {
    fun sendDlqAlert(dlqEvent: OutboxEventDlq)
    fun sendDlqThresholdAlert(unresolvedCount: Long, threshold: Long)
}
