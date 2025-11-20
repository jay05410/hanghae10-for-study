package io.hhplus.ecommerce.common.queue

/**
 * Queue 처리 인터페이스
 *
 * @param T Queue 아이템 타입
 * @param R 처리 결과 타입
 */
interface QueueProcessor<T, R> {

    /**
     * Queue에서 다음 작업 가져오기
     */
    fun dequeue(): T?

    /**
     * 작업 처리
     */
    fun process(item: T): R

    /**
     * 처리 성공 시 호출
     */
    fun onSuccess(item: T, result: R)

    /**
     * 처리 실패 시 호출
     */
    fun onFailure(item: T, error: Exception)
}
