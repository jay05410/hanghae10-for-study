package io.hhplus.ecommerce.order.application

import io.hhplus.ecommerce.common.queue.BaseQueueWorker
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderQueueRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 주문 생성 Queue Worker
 */
@Component
@ConditionalOnProperty(
    prefix = "order.queue.worker",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class OrderQueueWorker(
    private val orderQueueProcessor: OrderQueueProcessor,
    private val orderQueueService: OrderQueueService
) : BaseQueueWorker<OrderQueueRequest, Order>(orderQueueProcessor) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PROCESS_INTERVAL_MS = 10L // 더 빠른 처리를 위해 10ms로 단축
    }

    /**
     * Queue 처리 메인 루프 (50ms 주기)
     */
    @Scheduled(fixedDelay = PROCESS_INTERVAL_MS)
    fun process() {
        try {
            // 주문은 쿠폰과 다르게 단일 큐를 사용하므로 직접 처리
            processQueue()
        } catch (e: Exception) {
            logger.error("주문 Queue 처리 중 예상치 못한 오류", e)
        }
    }

    /**
     * 테스트용 수동 처리 메서드 - 강제로 모든 Queue 처리
     */
    fun forceProcessAllQueue() {
        logger.info("강제 Queue 처리 시작")
        var processedCount = 0
        var maxAttempts = 2000 // 무한루프 방지

        while (maxAttempts > 0) {
            try {
                processQueue()
                processedCount++
                maxAttempts--

                // Queue가 비었는지 확인
                val remainingQueueSize = orderQueueService.getQueueSize()
                if (remainingQueueSize <= 0) {
                    logger.info("Queue 처리 완료 - 총 ${processedCount}번 실행, 남은 Queue: $remainingQueueSize")
                    break
                }
            } catch (e: Exception) {
                logger.error("강제 Queue 처리 중 오류", e)
                maxAttempts--
            }
        }

        if (maxAttempts <= 0) {
            logger.warn("강제 Queue 처리 최대 시도 횟수 초과")
        }
    }
}