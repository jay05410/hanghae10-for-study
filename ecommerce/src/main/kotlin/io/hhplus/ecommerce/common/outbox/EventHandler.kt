package io.hhplus.ecommerce.common.outbox

/**
 * 이벤트 핸들러 인터페이스
 *
 * 모든 도메인 이벤트 핸들러가 구현해야 하는 인터페이스
 */
interface EventHandler {
    /**
     * 처리 가능한 이벤트 타입 반환
     */
    fun supportedEventTypes(): List<String>

    /**
     * 이벤트 처리
     *
     * @param event Outbox 이벤트
     * @return 처리 성공 여부
     */
    fun handle(event: OutboxEvent): Boolean

    /**
     * 배치 처리 지원 여부
     *
     * true인 경우 handleBatch()가 호출되어 여러 이벤트를 한 번에 처리
     * Pipeline 등 bulk 연산 최적화에 활용
     *
     * @return 배치 처리 지원 여부 (기본값: false)
     */
    fun supportsBatchProcessing(): Boolean = false

    /**
     * 이벤트 배치 처리
     *
     * supportsBatchProcessing()이 true인 경우에만 호출됨
     * Pipeline 등을 활용하여 여러 이벤트를 효율적으로 처리
     *
     * @param events 처리할 이벤트 목록
     * @return 처리 성공 여부
     */
    fun handleBatch(events: List<OutboxEvent>): Boolean = events.all { handle(it) }
}
