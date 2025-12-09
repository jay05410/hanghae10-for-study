package io.hhplus.ecommerce.common.outbox

import io.hhplus.ecommerce.common.outbox.dlq.DlqService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Outbox 이벤트 프로세서 - 이벤트 처리의 중앙 허브
 *
 * 역할:
 * - 미처리 이벤트 폴링
 * - 적절한 핸들러에게 이벤트 라우팅
 * - 처리 결과 기록 (성공/실패)
 * - 최대 재시도 초과 시 DLQ로 이동
 *
 * 이벤트 흐름:
 * 1. Outbox 테이블에서 미처리 이벤트 조회
 * 2. EventHandlerRegistry에서 적절한 핸들러 조회
 * 3. 핸들러에게 이벤트 전달
 * 4. 처리 결과에 따라 상태 업데이트
 * 5. 최대 재시도 초과 시 DLQ로 이동
 *
 * DLQ 연동:
 * - retryCount >= maxRetryCount 시 DLQ로 이동
 * - 핸들러 없는 이벤트도 DLQ로 이동
 */
@Component
class OutboxEventProcessor(
    private val outboxEventService: OutboxEventService,
    private val eventHandlerRegistry: EventHandlerRegistry,
    private val dlqService: DlqService
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val BATCH_SIZE = 50
    }

    /**
     * 5초마다 미처리 이벤트 처리
     */
    @Scheduled(fixedDelay = 5000)
    fun processEvents() {
        val events = outboxEventService.getUnprocessedEvents(BATCH_SIZE)

        if (events.isEmpty()) {
            return
        }

        logger.debug("[이벤트프로세서] ${events.size}개 이벤트 처리 시작")

        // 이벤트 타입별로 그룹핑
        val eventsByType = events.groupBy { it.eventType }

        eventsByType.forEach { (eventType, typeEvents) ->
            processEventsByType(eventType, typeEvents)
        }

        logger.debug("[이벤트프로세서] 이벤트 처리 완료")
    }

    /**
     * 이벤트 타입별 처리
     *
     * 배치 처리 지원 핸들러가 있으면 handleBatch() 호출,
     * 그렇지 않으면 개별 handle() 호출
     */
    private fun processEventsByType(eventType: String, events: List<OutboxEvent>) {
        val handlers = eventHandlerRegistry.getHandlers(eventType)

        if (handlers.isEmpty()) {
            events.forEach { event ->
                handleMissingHandler(event, eventType)
            }
            return
        }

        // 배치 처리 지원 핸들러 찾기
        val batchHandlers = handlers.filter { it.supportsBatchProcessing() }
        val nonBatchHandlers = handlers.filter { !it.supportsBatchProcessing() }

        // 배치 처리 지원 핸들러는 handleBatch()로 일괄 처리
        if (batchHandlers.isNotEmpty()) {
            processBatchHandlers(events, batchHandlers)
        }

        // 배치 미지원 핸들러는 개별 처리
        if (nonBatchHandlers.isNotEmpty()) {
            events.forEach { event ->
                processEventWithHandlers(event, nonBatchHandlers)
            }
        }
    }

    /**
     * 핸들러가 없는 이벤트 처리
     *
     * 핸들러가 없는 이벤트는 즉시 DLQ로 이동
     */
    private fun handleMissingHandler(event: OutboxEvent, eventType: String) {
        val errorMessage = "핸들러를 찾을 수 없습니다: $eventType"
        logger.warn("[이벤트프로세서] 핸들러 없음: eventType=$eventType, eventId=${event.id}")
        dlqService.moveToDlq(event, errorMessage)
    }

    /**
     * 배치 핸들러로 이벤트 일괄 처리
     */
    private fun processBatchHandlers(events: List<OutboxEvent>, handlers: List<EventHandler>) {
        var allSuccess = true
        val failedHandlers = mutableListOf<String>()

        handlers.forEach { handler ->
            try {
                val success = handler.handleBatch(events)
                if (!success) {
                    allSuccess = false
                    failedHandlers.add(handler::class.simpleName ?: "Unknown")
                    logger.warn("[이벤트프로세서] 배치 핸들러 처리 실패: handler=${handler::class.simpleName}")
                }
            } catch (e: Exception) {
                allSuccess = false
                failedHandlers.add(handler::class.simpleName ?: "Unknown")
                logger.error("[이벤트프로세서] 배치 핸들러 오류: handler=${handler::class.simpleName}, error=${e.message}", e)
            }
        }

        // 배치 처리 결과에 따라 이벤트 상태 업데이트
        events.forEach { event ->
            if (allSuccess) {
                outboxEventService.markAsProcessed(event.id)
            } else {
                handleFailure(event, "배치 핸들러 실패: ${failedHandlers.joinToString(", ")}")
            }
        }

        if (allSuccess) {
            logger.debug("[이벤트프로세서] 배치 처리 성공: eventCount=${events.size}, handlers=${handlers.size}")
        } else {
            logger.warn("[이벤트프로세서] 배치 처리 부분 실패: eventCount=${events.size}, 실패 핸들러=${failedHandlers}")
        }
    }

    /**
     * 개별 이벤트 처리 - 지정된 핸들러들을 순차적으로 호출
     */
    private fun processEventWithHandlers(event: OutboxEvent, handlers: List<EventHandler>) {
        try {
            var allSuccess = true
            val failedHandlers = mutableListOf<String>()

            handlers.forEach { handler ->
                try {
                    val success = handler.handle(event)
                    if (!success) {
                        allSuccess = false
                        failedHandlers.add(handler::class.simpleName ?: "Unknown")
                        logger.warn("[이벤트프로세서] 핸들러 처리 실패: handler=${handler::class.simpleName}, eventType=${event.eventType}")
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    failedHandlers.add(handler::class.simpleName ?: "Unknown")
                    logger.error("[이벤트프로세서] 핸들러 오류: handler=${handler::class.simpleName}, error=${e.message}", e)
                }
            }

            if (allSuccess) {
                outboxEventService.markAsProcessed(event.id)
                logger.debug("[이벤트프로세서] 이벤트 처리 성공: eventType=${event.eventType}, eventId=${event.id}")
            } else {
                handleFailure(event, "일부 핸들러 실패: ${failedHandlers.joinToString(", ")}")
            }

        } catch (e: Exception) {
            logger.error("[이벤트프로세서] 이벤트 처리 중 오류: eventType=${event.eventType}, eventId=${event.id}, error=${e.message}", e)
            handleFailure(event, e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 실패 처리 - DLQ 이동 또는 재시도
     *
     * retryCount >= maxRetryCount 시 DLQ로 이동
     * 그렇지 않으면 재시도 횟수 증가 후 다음 폴링에서 재시도
     */
    private fun handleFailure(event: OutboxEvent, errorMessage: String) {
        if (dlqService.shouldMoveToDlq(event)) {
            logger.warn("[이벤트프로세서] DLQ로 이동: eventId=${event.id}, retryCount=${event.retryCount}")
            dlqService.moveToDlq(event, errorMessage)
        } else {
            logger.warn(
                "[이벤트프로세서] 재시도 예정: eventId=${event.id}, " +
                    "retryCount=${event.retryCount + 1}, error=$errorMessage"
            )
            outboxEventService.incrementRetryAndMarkFailed(event.id, errorMessage)
        }
    }
}
