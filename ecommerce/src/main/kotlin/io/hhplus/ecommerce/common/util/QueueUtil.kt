package io.hhplus.ecommerce.common.util

import mu.KotlinLogging

/**
 * Queue Worker 유틸리티
 *
 * 역할:
 * - dequeue → process → success/failure 플로우 자동화
 * - 배치 처리 로직 제공
 */
object QueueUtil {
    private val logger = KotlinLogging.logger {}
    private const val DEFAULT_BATCH_SIZE = 10

    /**
     * Queue 처리 메인 로직 (배치 처리)
     *
     * @param dequeue Queue에서 항목 가져오는 함수
     * @param process 비즈니스 로직 처리 함수
     * @param onSuccess 성공 시 호출되는 함수
     * @param onFailure 실패 시 호출되는 함수
     * @param batchSize 배치 크기 (기본값: MAX_BATCH_SIZE)
     */
    fun <T, R> processQueueBatch(
        dequeue: () -> T?,
        process: (T) -> R,
        onSuccess: (T, R) -> Unit,
        onFailure: (T, Exception) -> Unit,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ) {
        try {
            repeat(batchSize) {
                val item = dequeue() ?: return@repeat

                try {
                    val result = process(item)
                    onSuccess(item, result)
                } catch (e: Exception) {
                    onFailure(item, e)
                }
            }
        } catch (e: Exception) {
            logger.error("Queue 배치 처리 중 예상치 못한 오류", e)
        }
    }

    /**
     * Queue 단일 처리 로직
     *
     * @param dequeue Queue에서 항목 가져오는 함수
     * @param process 비즈니스 로직 처리 함수
     * @param onSuccess 성공 시 호출되는 함수
     * @param onFailure 실패 시 호출되는 함수
     */
    fun <T, R> processQueueSingle(
        dequeue: () -> T?,
        process: (T) -> R,
        onSuccess: (T, R) -> Unit,
        onFailure: (T, Exception) -> Unit
    ) {
        try {
            val item = dequeue() ?: return

            try {
                val result = process(item)
                onSuccess(item, result)
            } catch (e: Exception) {
                onFailure(item, e)
            }
        } catch (e: Exception) {
            logger.error("Queue 단일 처리 중 예상치 못한 오류", e)
        }
    }
}