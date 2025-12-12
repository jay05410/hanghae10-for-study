package io.hhplus.ecommerce.common.outbox.dlq.infra

import io.hhplus.ecommerce.common.outbox.dlq.AlertService
import io.hhplus.ecommerce.common.outbox.dlq.OutboxEventDlq
import mu.KotlinLogging
import org.springframework.stereotype.Service

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
