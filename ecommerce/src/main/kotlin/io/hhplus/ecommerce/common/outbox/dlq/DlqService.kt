package io.hhplus.ecommerce.common.outbox.dlq

import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Dead Letter Queue 서비스
 *
 * 역할:
 * - 실패한 이벤트를 DLQ로 이동
 * - DLQ 이벤트 재처리
 * - DLQ 이벤트 해결 처리
 * - 임계치 모니터링 및 알림
 */
@Service
class DlqService(
    private val dlqRepository: OutboxEventDlqRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val alertService: AlertService,
    @Value("\${outbox.max-retry-count:5}")
    private val maxRetryCount: Int = 5,
    @Value("\${outbox.dlq.alert-threshold:10}")
    private val alertThreshold: Long = 10
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 이벤트를 DLQ로 이동
     *
     * @param event 원본 Outbox 이벤트
     * @param errorMessage 실패 사유
     * @return 생성된 DLQ 이벤트
     */
    @Transactional
    fun moveToDlq(event: OutboxEvent, errorMessage: String): OutboxEventDlq {
        logger.warn("[DLQ] 이벤트 이동 시작: eventId=${event.id}, type=${event.eventType}")

        // 1. DLQ 이벤트 생성 및 저장
        val dlqEvent = OutboxEventDlq.fromOutboxEvent(event, errorMessage)
        val savedDlqEvent = dlqRepository.save(dlqEvent)

        // 2. 원본 이벤트 삭제
        outboxEventRepository.deleteById(event.id)

        // 3. 알림 발송
        alertService.sendDlqAlert(savedDlqEvent)

        // 4. 임계치 체크 및 추가 알림
        checkThresholdAndAlert()

        logger.warn(
            "[DLQ] 이벤트 이동 완료: eventId=${event.id}, dlqId=${savedDlqEvent.id}, error=$errorMessage"
        )

        return savedDlqEvent
    }

    /**
     * 이벤트가 DLQ로 이동해야 하는지 판단
     *
     * @param event Outbox 이벤트
     * @return DLQ 이동 필요 여부
     */
    fun shouldMoveToDlq(event: OutboxEvent): Boolean {
        return event.retryCount >= maxRetryCount
    }

    /**
     * DLQ 이벤트 재처리
     *
     * DLQ 이벤트를 새 Outbox 이벤트로 복원하여 재처리 대상에 포함
     *
     * @param dlqEventId DLQ 이벤트 ID
     * @param operatorId 처리자 ID
     * @return 재처리 성공 여부
     */
    @Transactional
    fun retryFromDlq(dlqEventId: Long, operatorId: String): Boolean {
        val dlqEvent = dlqRepository.findById(dlqEventId)
            ?: throw IllegalArgumentException("DLQ 이벤트를 찾을 수 없습니다: $dlqEventId")

        if (dlqEvent.resolved) {
            throw IllegalStateException("이미 해결된 DLQ 이벤트입니다: $dlqEventId")
        }

        logger.info("[DLQ] 재처리 시작: dlqId=$dlqEventId, operator=$operatorId")

        // 1. 새 Outbox 이벤트 생성 (retryCount=0으로 초기화)
        val newEvent = dlqEvent.toOutboxEvent()
        outboxEventRepository.save(newEvent)

        // 2. DLQ 이벤트 해결 처리
        dlqEvent.resolve(operatorId, "재처리를 위해 Outbox로 복원됨")
        dlqRepository.save(dlqEvent)

        logger.info("[DLQ] 재처리 완료: dlqId=$dlqEventId, newEventId=${newEvent.id}")

        return true
    }

    /**
     * DLQ 이벤트 수동 해결 처리
     *
     * 재처리 없이 해결 처리 (수동 처리 완료, 더 이상 필요 없음 등)
     *
     * @param dlqEventId DLQ 이벤트 ID
     * @param operatorId 처리자 ID
     * @param note 해결 메모
     */
    @Transactional
    fun resolveManually(dlqEventId: Long, operatorId: String, note: String) {
        val dlqEvent = dlqRepository.findById(dlqEventId)
            ?: throw IllegalArgumentException("DLQ 이벤트를 찾을 수 없습니다: $dlqEventId")

        if (dlqEvent.resolved) {
            throw IllegalStateException("이미 해결된 DLQ 이벤트입니다: $dlqEventId")
        }

        dlqEvent.resolve(operatorId, note)
        dlqRepository.save(dlqEvent)

        logger.info("[DLQ] 수동 해결 완료: dlqId=$dlqEventId, operator=$operatorId, note=$note")
    }

    /**
     * 미해결 DLQ 이벤트 목록 조회
     */
    fun getUnresolvedEvents(): List<OutboxEventDlq> {
        return dlqRepository.findUnresolvedEvents()
    }

    /**
     * 미해결 DLQ 이벤트 수 조회
     */
    fun countUnresolved(): Long {
        return dlqRepository.countUnresolved()
    }

    /**
     * 임계치 체크 및 알림
     */
    private fun checkThresholdAndAlert() {
        val unresolvedCount = dlqRepository.countUnresolved()
        if (unresolvedCount >= alertThreshold) {
            alertService.sendDlqThresholdAlert(unresolvedCount, alertThreshold)
        }
    }
}
