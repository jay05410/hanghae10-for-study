package io.hhplus.ecommerce.coupon.domain.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import java.time.LocalDateTime

/**
 * 쿠폰 발급 Queue 요청 도메인 모델
 *
 * 역할:
 * - 쿠폰 발급 요청을 Queue에 등록하고 상태 관리
 * - 대기 순번 추적 및 처리 결과 기록
 *
 * 비즈니스 규칙:
 * - 모든 쿠폰 발급 요청은 Queue에 등록되어야 함
 * - 대기 순번은 요청 순서대로 부여됨
 * - 처리 시간 초과 시 자동 만료됨 (기본 10분)
 *
 * Redis 저장 구조:
 * - Key: "coupon:queue:request:{queueId}"
 * - Value: JSON 직렬화된 CouponQueueRequest
 * - Queue: "coupon:queue:waiting:{couponId}" (WAITING 상태 요청 목록)
 */
data class CouponQueueRequest(
    val queueId: String,
    val userId: Long,
    val couponId: Long,
    val couponName: String = "",
    var queuePosition: Int = 0,
    var status: QueueStatus = QueueStatus.WAITING,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    var processedAt: LocalDateTime? = null,
    var completedAt: LocalDateTime? = null,
    var failureReason: String? = null,
    var userCouponId: Long? = null
) {
    /**
     * Queue 처리 시작
     */
    fun startProcessing() {
        this.status = QueueStatus.PROCESSING
        this.processedAt = LocalDateTime.now()
    }

    /**
     * Queue 처리 성공
     *
     * @param userCouponId 발급된 사용자 쿠폰 ID
     */
    fun complete(userCouponId: Long) {
        this.status = QueueStatus.COMPLETED
        this.completedAt = LocalDateTime.now()
        this.userCouponId = userCouponId
    }

    /**
     * Queue 처리 실패
     *
     * @param reason 실패 사유
     */
    fun fail(reason: String) {
        this.status = QueueStatus.FAILED
        this.completedAt = LocalDateTime.now()
        this.failureReason = reason
    }

    /**
     * Queue 요청 만료 처리
     */
    fun expire() {
        this.status = QueueStatus.EXPIRED
        this.completedAt = LocalDateTime.now()
        this.failureReason = "대기 시간 초과"
    }

    /**
     * 예상 대기 시간 계산 (초 단위)
     */
    @JsonIgnore
    fun getEstimatedWaitingTimeSeconds(averageProcessingTimeMs: Long = 100): Long {
        return (queuePosition * averageProcessingTimeMs) / 1000
    }

    /**
     * Queue가 아직 처리 가능한 상태인지 확인 (완료/실패/만료 아님)
     */
    @JsonIgnore
    fun isActive(): Boolean = status !in setOf(QueueStatus.COMPLETED, QueueStatus.FAILED, QueueStatus.EXPIRED)

    companion object {
        /**
         * 새로운 Queue 요청 생성
         *
         * @param queueId Queue ID (Snowflake ID)
         * @param userId 사용자 ID
         * @param couponId 쿠폰 ID
         * @param couponName 쿠폰명 (선택)
         * @param queuePosition 대기 순번
         * @return 생성된 Queue 요청
         */
        fun create(
            queueId: String,
            userId: Long,
            couponId: Long,
            couponName: String = "",
            queuePosition: Int
        ): CouponQueueRequest {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(couponId > 0) { "쿠폰 ID는 유효해야 합니다" }
            require(queuePosition >= 0) { "대기 순번은 0 이상이어야 합니다" }

            return CouponQueueRequest(
                queueId = queueId,
                userId = userId,
                couponId = couponId,
                couponName = couponName,
                queuePosition = queuePosition
            )
        }
    }
}
