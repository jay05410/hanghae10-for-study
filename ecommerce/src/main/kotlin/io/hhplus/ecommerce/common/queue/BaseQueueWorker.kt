package io.hhplus.ecommerce.common.queue

import mu.KotlinLogging

/**
 * Queue Worker 추상 클래스
 *
 * 역할:
 * - dequeue → process → success/failure 플로우 자동화
 */
abstract class BaseQueueWorker<T, R>(
    private val processor: QueueProcessor<T, R>
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Queue 처리 메인 로직 (배치 처리)
     *
     * 테스트에서도 사용할 수 있도록 public으로 선언
     */
    fun processQueue() {
        try {
            // 배치 처리: 한 번에 최대 50개까지 처리
            repeat(50) {
                val item = processor.dequeue() ?: return@repeat

                try {
                    val result = processor.process(item)
                    processor.onSuccess(item, result)
                } catch (e: Exception) {
                    processor.onFailure(item, e)
                }
            }
        } catch (e: Exception) {
            logger.error("Queue 처리 중 예상치 못한 오류", e)
        }
    }
}
