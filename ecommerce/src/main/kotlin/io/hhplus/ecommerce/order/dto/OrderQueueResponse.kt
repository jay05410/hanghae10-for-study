package io.hhplus.ecommerce.order.dto

import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.order.domain.entity.OrderQueueRequest
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** Controller 응답 DTO */
@Schema(description = "주문 대기열 상태 응답")
data class OrderQueueResponse(
    @Schema(description = "Queue ID", example = "1234567890123456")
    val queueId: String,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "대기 순번", example = "5")
    val queuePosition: Int,

    @Schema(description = "Queue 상태", example = "WAITING")
    val status: QueueStatus,

    @Schema(description = "요청 시간")
    val requestedAt: LocalDateTime,

    @Schema(description = "처리 시작 시간")
    val processedAt: LocalDateTime? = null,

    @Schema(description = "완료 시간")
    val completedAt: LocalDateTime? = null,

    @Schema(description = "실패 사유")
    val failureReason: String? = null,

    @Schema(description = "생성된 주문 ID (완료 시)")
    val orderId: Long? = null,

    @Schema(description = "예상 대기 시간(초)")
    val estimatedWaitingTimeSeconds: Long
)

/**
 * OrderQueueRequest 도메인 엔티티를 OrderQueueResponse DTO로 변환
 */
fun OrderQueueRequest.toResponse(): OrderQueueResponse {
    return OrderQueueResponse(
        queueId = queueId,
        userId = userId,
        queuePosition = queuePosition,
        status = status,
        requestedAt = requestedAt,
        processedAt = processedAt,
        completedAt = completedAt,
        failureReason = failureReason,
        orderId = orderId,
        estimatedWaitingTimeSeconds = getEstimatedWaitingTimeSeconds()
    )
}