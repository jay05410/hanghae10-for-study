package io.hhplus.ecommerce.order.domain.eventsourcing

/**
 * 주문 이벤트 저장소 인터페이스 (Event Sourcing)
 *
 * 도메인 레이어에서 정의하는 이벤트 저장소 인터페이스
 * 인프라 레이어에서 구현체 제공
 */
interface OrderEventRepository {

    /**
     * 이벤트 저장
     *
     * @param event 저장할 이벤트
     * @param expectedVersion 예상 버전 (낙관적 잠금)
     * @return 저장된 이벤트 엔티티
     * @throws OptimisticLockException 버전 충돌 시
     */
    fun save(event: OrderEvent, expectedVersion: Int): OrderEventRecord

    /**
     * 여러 이벤트 일괄 저장
     *
     * @param events 저장할 이벤트 목록
     * @param aggregateId Aggregate ID
     * @param expectedVersion 예상 시작 버전
     * @return 저장된 이벤트 레코드 목록
     */
    fun saveAll(events: List<OrderEvent>, aggregateId: Long, expectedVersion: Int): List<OrderEventRecord>

    /**
     * 특정 Aggregate의 모든 이벤트 조회
     *
     * @param aggregateId Aggregate ID (Order ID)
     * @return 버전 순으로 정렬된 이벤트 목록
     */
    fun findByAggregateId(aggregateId: Long): List<OrderEvent>

    /**
     * 특정 버전 이후의 이벤트 조회
     * (스냅샷 이후 이벤트만 조회할 때 사용)
     *
     * @param aggregateId Aggregate ID
     * @param afterVersion 이 버전 이후의 이벤트만 조회
     * @return 버전 순으로 정렬된 이벤트 목록
     */
    fun findByAggregateIdAfterVersion(aggregateId: Long, afterVersion: Int): List<OrderEvent>

    /**
     * 특정 Aggregate의 현재 버전 조회
     *
     * @param aggregateId Aggregate ID
     * @return 현재 버전 (이벤트 없으면 0)
     */
    fun getCurrentVersion(aggregateId: Long): Int

    /**
     * 특정 Aggregate의 이벤트 개수 조회
     *
     * @param aggregateId Aggregate ID
     * @return 이벤트 개수
     */
    fun countByAggregateId(aggregateId: Long): Long

    /**
     * 특정 Aggregate에 이벤트가 존재하는지 확인
     *
     * @param aggregateId Aggregate ID
     * @return 존재 여부
     */
    fun existsByAggregateId(aggregateId: Long): Boolean
}

/**
 * 저장된 이벤트 레코드
 *
 * 이벤트 저장 후 반환되는 메타데이터 포함 레코드
 */
data class OrderEventRecord(
    val id: Long,
    val aggregateId: Long,
    val eventType: String,
    val version: Int,
    val event: OrderEvent
)
