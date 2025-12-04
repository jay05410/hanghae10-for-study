package io.hhplus.ecommerce.common.outbox

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Outbox 이벤트 프로세서 - 이벤트 처리의 중앙 허브
 *
 * 역할:
 * - 미처리 이벤트 폴링
 * - 적절한 핸들러에게 이벤트 라우팅
 * - 처리 결과 기록 (성공/실패)
 *
 * 이벤트 흐름:
 * 1. Outbox 테이블에서 미처리 이벤트 조회
 * 2. EventHandlerRegistry에서 적절한 핸들러 조회
 * 3. 핸들러에게 이벤트 전달
 * 4. 처리 결과에 따라 상태 업데이트
 */
@Component
class OutboxEventProcessor(
    private val outboxEventService: OutboxEventService,
    private val eventHandlerRegistry: EventHandlerRegistry
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

        events.forEach { event ->
            processEvent(event)
        }

        logger.debug("[이벤트프로세서] 이벤트 처리 완료")
    }

    /**
     * 개별 이벤트 처리 - 모든 등록된 핸들러를 순차적으로 호출
     */
    private fun processEvent(event: OutboxEvent) {
        try {
            val handlers = eventHandlerRegistry.getHandlers(event.eventType)

            if (handlers.isEmpty()) {
                logger.warn("[이벤트프로세서] 핸들러 없음: eventType=${event.eventType}, eventId=${event.id}")
                outboxEventService.markAsFailed(event.id, "핸들러를 찾을 수 없습니다: ${event.eventType}")
                return
            }

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
                logger.debug("[이벤트프로세서] 이벤트 처리 성공: eventType=${event.eventType}, eventId=${event.id}, handlers=${handlers.size}")
            } else {
                outboxEventService.markAsFailed(event.id, "일부 핸들러 실패: ${failedHandlers.joinToString(", ")}")
                logger.warn("[이벤트프로세서] 이벤트 처리 부분 실패: eventType=${event.eventType}, 실패 핸들러=${failedHandlers}")
            }

        } catch (e: Exception) {
            logger.error("[이벤트프로세서] 이벤트 처리 중 오류: eventType=${event.eventType}, eventId=${event.id}, error=${e.message}", e)
            outboxEventService.markAsFailed(event.id, e.message ?: "알 수 없는 오류")
        }
    }
}
