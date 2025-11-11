package io.hhplus.ecommerce.common.outbox

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
//import jakarta.persistence.*
import java.time.LocalDateTime

//@Entity
//@Table(name = "outbox_event")
class OutboxEvent(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @Column(nullable = false, length = 100)
    val eventType: String,

//    @Column(nullable = false, length = 100)
    val aggregateType: String,

//    @Column(nullable = false)
    val aggregateId: String,

//    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

//    @Column(nullable = false)
    var processed: Boolean = false,

//    @Column(nullable = true)
    var processedAt: LocalDateTime? = null,

//    @Column(nullable = true, columnDefinition = "TEXT")
    var errorMessage: String? = null,

//    @Column(nullable = false)
    var retryCount: Int = 0
) : ActiveJpaEntity() {

    fun markAsProcessed(processedAt: LocalDateTime = LocalDateTime.now()) {
        require(!processed) { "이미 처리된 이벤트입니다" }
        this.processed = true
        this.processedAt = processedAt
        this.errorMessage = null
    }

    fun incrementRetryCount() {
        require(retryCount < 5) { "최대 재시도 횟수를 초과했습니다" }
        this.retryCount++
    }

    fun markAsFailed(errorMessage: String, failedAt: LocalDateTime = LocalDateTime.now()) {
        require(errorMessage.isNotBlank()) { "에러 메시지는 필수입니다" }
        this.errorMessage = errorMessage
        this.processed = false
        this.processedAt = failedAt
    }

    companion object {
        fun create(
            eventType: String,
            aggregateType: String,
            aggregateId: String,
            payload: String,
            createdBy: Long
        ): OutboxEvent {
            require(eventType.isNotBlank()) { "이벤트 타입은 필수입니다" }
            require(aggregateType.isNotBlank()) { "애그리게이트 타입은 필수입니다" }
            require(aggregateId.isNotBlank()) { "애그리게이트 ID는 필수입니다" }
            require(payload.isNotBlank()) { "페이로드는 필수입니다" }

            return OutboxEvent(
                eventType = eventType,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                payload = payload
            )
        }
    }
}