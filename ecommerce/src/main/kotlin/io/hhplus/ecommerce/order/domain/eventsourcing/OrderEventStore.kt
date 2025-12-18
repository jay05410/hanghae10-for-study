package io.hhplus.ecommerce.order.domain.eventsourcing

import io.hhplus.ecommerce.common.messaging.MessagePublisher
import io.hhplus.ecommerce.common.messaging.Topics
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Order Event Store (Event Sourcing)
 *
 * 주문 도메인의 Event Sourcing을 위한 핵심 서비스
 *
 * 책임:
 * - Aggregate 저장 및 로드
 * - 이벤트 발행 (MessagePublisher)
 * - 스냅샷 관리 (성능 최적화)
 *
 * 사용 패턴:
 * 1. 새 주문 생성: createOrder() → Aggregate 생성 + 이벤트 저장 + 발행
 * 2. 기존 주문 조회: load() → 이벤트 재생으로 Aggregate 복원
 * 3. 주문 상태 변경: save() → 변경 이벤트 저장 + 발행
 */
@Service
class OrderEventStore(
    private val orderEventRepository: OrderEventRepository,
    private val messagePublisher: MessagePublisher,
    private val snapshotRepository: OrderSnapshotRepository? = null // 선택적 스냅샷 저장소
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        // 스냅샷 생성 임계치 (이벤트 개수)
        private const val SNAPSHOT_THRESHOLD = 50
    }

    /**
     * Aggregate 로드
     *
     * 이벤트 재생으로 현재 상태 복원
     * 스냅샷이 있으면 스냅샷 이후 이벤트만 재생
     */
    @Transactional(readOnly = true)
    fun load(orderId: Long): OrderAggregate {
        // 스냅샷 확인 (있으면 사용)
        val snapshot = snapshotRepository?.findLatestByAggregateId(orderId)

        val events = if (snapshot != null) {
            orderEventRepository.findByAggregateIdAfterVersion(orderId, snapshot.version)
        } else {
            orderEventRepository.findByAggregateId(orderId)
        }

        if (events.isEmpty() && snapshot == null) {
            throw OrderNotFoundException("주문을 찾을 수 없습니다: $orderId")
        }

        return if (snapshot != null) {
            OrderAggregate.fromSnapshot(snapshot, events)
        } else {
            OrderAggregate.rebuild(events)
        }
    }

    /**
     * Aggregate 저장
     *
     * 미발행 이벤트를 저장하고 발행
     */
    @Transactional
    fun save(aggregate: OrderAggregate): OrderAggregate {
        val uncommittedEvents = aggregate.uncommittedEvents
        if (uncommittedEvents.isEmpty()) {
            logger.debug { "저장할 이벤트가 없습니다: orderId=${aggregate.id}" }
            return aggregate
        }

        val expectedVersion = aggregate.version - uncommittedEvents.size

        // 이벤트 저장
        val savedRecords = orderEventRepository.saveAll(
            events = uncommittedEvents,
            aggregateId = aggregate.id,
            expectedVersion = expectedVersion
        )

        logger.info {
            "이벤트 저장 완료: orderId=${aggregate.id}, " +
                "eventCount=${savedRecords.size}, " +
                "newVersion=${aggregate.version}"
        }

        // 이벤트 발행
        savedRecords.forEach { record ->
            publishEvent(record.event)
        }

        // 미발행 이벤트 목록 초기화
        aggregate.clearUncommittedEvents()

        // 스냅샷 필요 여부 확인
        checkAndCreateSnapshot(aggregate)

        return aggregate
    }

    /**
     * 새 주문 생성 (편의 메서드)
     */
    @Transactional
    fun createOrder(
        orderId: Long,
        orderNumber: String,
        userId: Long,
        totalAmount: Long,
        discountAmount: Long = 0,
        usedCouponIds: List<Long> = emptyList(),
        items: List<OrderItemSnapshot>
    ): OrderAggregate {
        // 중복 체크
        if (orderEventRepository.existsByAggregateId(orderId)) {
            throw OrderAlreadyExistsException("이미 존재하는 주문입니다: $orderId")
        }

        val aggregate = OrderAggregate.create(
            orderId = orderId,
            orderNumber = orderNumber,
            userId = userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            usedCouponIds = usedCouponIds,
            items = items
        )

        return save(aggregate)
    }

    /**
     * 주문 확정 (편의 메서드)
     */
    @Transactional
    fun confirmOrder(orderId: Long, paymentId: Long? = null, confirmedBy: String = "SYSTEM"): OrderAggregate {
        val aggregate = load(orderId)
        aggregate.confirm(paymentId, confirmedBy)
        return save(aggregate)
    }

    /**
     * 주문 완료 (편의 메서드)
     */
    @Transactional
    fun completeOrder(orderId: Long, completedBy: String = "SYSTEM"): OrderAggregate {
        val aggregate = load(orderId)
        aggregate.complete(completedBy)
        return save(aggregate)
    }

    /**
     * 주문 취소 (편의 메서드)
     */
    @Transactional
    fun cancelOrder(
        orderId: Long,
        reason: String,
        cancelledBy: String,
        refundAmount: Long? = null
    ): OrderAggregate {
        val aggregate = load(orderId)
        aggregate.cancel(reason, cancelledBy, refundAmount)
        return save(aggregate)
    }

    /**
     * 주문 실패 (편의 메서드)
     */
    @Transactional
    fun failOrder(orderId: Long, reason: String, errorCode: String? = null): OrderAggregate {
        val aggregate = load(orderId)
        aggregate.fail(reason, errorCode)
        return save(aggregate)
    }

    /**
     * 특정 시점의 Aggregate 상태 조회 (Temporal Query)
     */
    @Transactional(readOnly = true)
    fun loadAtVersion(orderId: Long, targetVersion: Int): OrderAggregate {
        val allEvents = orderEventRepository.findByAggregateId(orderId)
        if (allEvents.isEmpty()) {
            throw OrderNotFoundException("주문을 찾을 수 없습니다: $orderId")
        }

        val eventsUpToVersion = allEvents.take(targetVersion)
        if (eventsUpToVersion.isEmpty()) {
            throw IllegalArgumentException("유효하지 않은 버전입니다: $targetVersion")
        }

        return OrderAggregate.rebuild(eventsUpToVersion)
    }

    /**
     * 이벤트 발행 (MessagePublisher)
     */
    private fun publishEvent(event: OrderEvent) {
        try {
            messagePublisher.publish(
                topic = Topics.ORDER,
                key = event.orderId.toString(),
                payload = event
            )
            logger.debug { "이벤트 발행: type=${event.eventType}, orderId=${event.orderId}" }
        } catch (e: Exception) {
            logger.error(e) { "이벤트 발행 실패: type=${event.eventType}, orderId=${event.orderId}" }
            // 발행 실패 시에도 저장은 완료되었으므로 예외를 던지지 않음
            // Outbox 패턴과 함께 사용하면 재발행 가능
        }
    }

    /**
     * 스냅샷 생성 필요 여부 확인 및 생성
     */
    private fun checkAndCreateSnapshot(aggregate: OrderAggregate) {
        if (snapshotRepository == null) return

        val lastSnapshotVersion = snapshotRepository.findLatestByAggregateId(aggregate.id)?.version ?: 0

        // 마지막 스냅샷 이후 이벤트가 임계치 이상이면 새 스냅샷 생성
        if (aggregate.version - lastSnapshotVersion >= SNAPSHOT_THRESHOLD) {
            val snapshot = aggregate.toSnapshot()
            snapshotRepository.save(snapshot)
            logger.info { "스냅샷 생성: orderId=${aggregate.id}, version=${aggregate.version}" }
        }
    }
}

/**
 * 주문을 찾을 수 없는 경우 예외
 */
class OrderNotFoundException(message: String) : RuntimeException(message)

/**
 * 이미 존재하는 주문인 경우 예외
 */
class OrderAlreadyExistsException(message: String) : RuntimeException(message)

/**
 * 주문 스냅샷 저장소 인터페이스 (선택적)
 *
 * 성능 최적화를 위한 스냅샷 저장소
 * 구현하지 않아도 Event Sourcing은 정상 동작
 */
interface OrderSnapshotRepository {
    fun save(snapshot: OrderSnapshot): OrderSnapshot
    fun findLatestByAggregateId(aggregateId: Long): OrderSnapshot?
}
