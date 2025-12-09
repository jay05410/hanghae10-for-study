package io.hhplus.ecommerce.common.outbox.dlq

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.common.outbox.OutboxEvent
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Dead Letter Queue 엔티티
 *
 * 최대 재시도 횟수를 초과한 실패 이벤트를 저장
 * 운영자가 수동으로 검토 후 재처리하거나 해결 처리
 */
@Entity
@Table(name = "outbox_event_dlq")
class OutboxEventDlq(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val originalEventId: Long,

    @Column(nullable = false, length = 100)
    val eventType: String,

    @Column(nullable = false, length = 100)
    val aggregateType: String,

    @Column(nullable = false)
    val aggregateId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val errorMessage: String,

    @Column(nullable = false)
    val failedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val retryCount: Int,

    @Column(nullable = false)
    var resolved: Boolean = false,

    @Column(nullable = true)
    var resolvedAt: LocalDateTime? = null,

    @Column(nullable = true)
    var resolvedBy: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    var resolutionNote: String? = null
) : BaseJpaEntity() {

    companion object {
        fun fromOutboxEvent(event: OutboxEvent, errorMessage: String): OutboxEventDlq {
            return OutboxEventDlq(
                originalEventId = event.id,
                eventType = event.eventType,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                payload = event.payload,
                errorMessage = errorMessage,
                retryCount = event.retryCount
            )
        }
    }

    /**
     * DLQ 이벤트 해결 처리
     *
     * @param resolvedBy 처리자 ID (운영자 이름 또는 시스템)
     * @param note 해결 메모 (선택)
     */
    fun resolve(resolvedBy: String, note: String? = null) {
        require(!resolved) { "이미 해결된 DLQ 이벤트입니다" }
        this.resolved = true
        this.resolvedAt = LocalDateTime.now()
        this.resolvedBy = resolvedBy
        this.resolutionNote = note
    }

    /**
     * 재처리를 위한 OutboxEvent 생성
     */
    fun toOutboxEvent(): OutboxEvent {
        return OutboxEvent.create(
            eventType = eventType,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payload = payload
        )
    }
}
