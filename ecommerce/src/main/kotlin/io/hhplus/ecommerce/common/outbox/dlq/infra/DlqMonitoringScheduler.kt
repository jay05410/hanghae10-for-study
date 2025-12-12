package io.hhplus.ecommerce.common.outbox.dlq.infra

import io.hhplus.ecommerce.common.outbox.dlq.AlertService
import io.hhplus.ecommerce.common.outbox.dlq.DlqService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * DLQ 모니터링 스케줄러
 *
 * 역할:
 * - 주기적으로 미해결 DLQ 이벤트 수 체크
 * - 임계치 초과 시 알림 발송
 */
@Component
class DlqMonitoringScheduler(
    private val dlqService: DlqService,
    private val alertService: AlertService,
    @Value("\${outbox.dlq.alert-threshold:10}")
    private val alertThreshold: Long = 10
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 60000)
    fun monitorDlqStatus() {
        val unresolvedCount = dlqService.countUnresolved()

        if (unresolvedCount == 0L) {
            logger.debug("[DLQ모니터링] 미해결 DLQ 이벤트 없음")
            return
        }

        logger.info("[DLQ모니터링] 미해결 DLQ 이벤트 수: $unresolvedCount")

        if (unresolvedCount >= alertThreshold) {
            alertService.sendDlqThresholdAlert(unresolvedCount, alertThreshold)
        }
    }

    @Scheduled(fixedDelay = 600000)
    fun logDlqReport() {
        val unresolvedEvents = dlqService.getUnresolvedEvents()

        if (unresolvedEvents.isEmpty()) {
            return
        }

        val report = buildString {
            appendLine()
            appendLine("================= DLQ STATUS REPORT =================")
            appendLine(" Total Unresolved : ${unresolvedEvents.size}")
            appendLine()
            appendLine(" By Event Type:")
            unresolvedEvents.groupBy { it.eventType }
                .forEach { (type, events) ->
                    appendLine("   - $type: ${events.size}건")
                }
            appendLine()
            appendLine(" Oldest Event:")
            unresolvedEvents.minByOrNull { it.failedAt }?.let { oldest ->
                appendLine("   - ID: ${oldest.id}")
                appendLine("   - Type: ${oldest.eventType}")
                appendLine("   - Failed At: ${oldest.failedAt}")
                appendLine("   - Error: ${oldest.errorMessage.take(100)}...")
            }
            appendLine("=====================================================")
        }

        logger.warn(report)
    }
}
