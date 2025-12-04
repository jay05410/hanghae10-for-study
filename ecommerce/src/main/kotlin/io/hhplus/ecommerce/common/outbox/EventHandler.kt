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
}
