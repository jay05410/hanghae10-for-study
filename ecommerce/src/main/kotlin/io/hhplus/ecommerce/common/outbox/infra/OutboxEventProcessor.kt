package io.hhplus.ecommerce.common.outbox.infra

import io.hhplus.ecommerce.common.outbox.OutboxEvent
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.common.outbox.dlq.DlqService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Outbox 이벤트 프로세서 (Fallback)
 *
 * CDC가 primary 처리 경로이며, 이 프로세서는 fallback으로 동작:
 * - CDC 처리 실패 시 누락된 이벤트 복구
 * - Debezium 장애 시 대체 처리
 *
 * 역할:
 * - 미처리 이벤트 폴링 (60초 주기)
 * - 적절한 핸들러에게 이벤트 라우팅
 * - 처리 결과 기록 (성공/실패)
 * - 최대 재시도 초과 시 DLQ로 이동
 *
 * @see OutboxCdcConsumer CDC 기반 실시간 처리
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

    @Scheduled(fixedDelay = 60000)  // 60초 (CDC fallback)
    fun processEvents() {
        val events = outboxEventService.getUnprocessedEvents(BATCH_SIZE)

        if (events.isEmpty()) {
            return
        }

        logger.debug("[이벤트프로세서] ${events.size}개 이벤트 처리 시작")

        val eventsByType = events.groupBy { it.eventType }

        eventsByType.forEach { (eventType, typeEvents) ->
            processEventsByType(eventType, typeEvents)
        }

        logger.debug("[이벤트프로세서] 이벤트 처리 완료")
    }

    private fun processEventsByType(eventType: String, events: List<OutboxEvent>) {
        val handlers = eventHandlerRegistry.getHandlers(eventType)

        if (handlers.isEmpty()) {
            events.forEach { event ->
                handleMissingHandler(event, eventType)
            }
            return
        }

        val batchHandlers = handlers.filter { it.supportsBatchProcessing() }
        val nonBatchHandlers = handlers.filter { !it.supportsBatchProcessing() }

        if (batchHandlers.isNotEmpty()) {
            processBatchHandlers(events, batchHandlers)
        }

        if (nonBatchHandlers.isNotEmpty()) {
            events.forEach { event ->
                processEventWithHandlers(event, nonBatchHandlers)
            }
        }
    }

    private fun handleMissingHandler(event: OutboxEvent, eventType: String) {
        val errorMessage = "핸들러를 찾을 수 없습니다: $eventType"
        logger.warn("[이벤트프로세서] 핸들러 없음: eventType=$eventType, eventId=${event.id}")
        dlqService.moveToDlq(event, errorMessage)
    }

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
                        // Handler 내부에서 상세 에러 로깅하므로 여기서는 debug만
                        logger.debug("[이벤트프로세서] 핸들러 처리 실패: handler={}, eventType={}", handler::class.simpleName, event.eventType)
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    failedHandlers.add(handler::class.simpleName ?: "Unknown")
                    // Handler가 예외를 throw한 경우에만 여기서 로깅
                    logger.error("[이벤트프로세서] 핸들러 예외: handler={}, eventType={}, error={}", handler::class.simpleName, event.eventType, e.message, e)
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
