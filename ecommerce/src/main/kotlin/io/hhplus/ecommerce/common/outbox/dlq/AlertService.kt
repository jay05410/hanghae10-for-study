package io.hhplus.ecommerce.common.outbox.dlq

import mu.KotlinLogging
import org.springframework.stereotype.Service

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

/**
 * 로깅 기반 알림 서비스 (기본 구현)
 *
 * 개발/테스트 환경에서 사용
 * 운영 환경에서는 SlackAlertService, EmailAlertService 등으로 교체
 */
@Service
class LoggingAlertService : AlertService {

    private val logger = KotlinLogging.logger {}

    override fun sendDlqAlert(dlqEvent: OutboxEventDlq) {
        logger.error(
            """
            |
            |===================== DLQ ALERT =====================
            | Event ID     : ${dlqEvent.originalEventId}
            | Event Type   : ${dlqEvent.eventType}
            | Aggregate    : ${dlqEvent.aggregateType}#${dlqEvent.aggregateId}
            | Retry Count  : ${dlqEvent.retryCount}
            | Error        : ${dlqEvent.errorMessage}
            | Failed At    : ${dlqEvent.failedAt}
            |=====================================================
            """.trimMargin()
        )
    }

    override fun sendDlqThresholdAlert(unresolvedCount: Long, threshold: Long) {
        logger.error(
            """
            |
            |================= DLQ THRESHOLD ALERT =================
            | Unresolved Count : $unresolvedCount
            | Threshold        : $threshold
            | Action Required  : 미처리 DLQ 이벤트가 임계치를 초과했습니다.
            |=======================================================
            """.trimMargin()
        )
    }
}
