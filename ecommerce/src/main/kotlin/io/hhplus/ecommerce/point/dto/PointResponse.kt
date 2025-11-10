package io.hhplus.ecommerce.point.dto

import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import java.time.LocalDateTime

/**
 * 사용자 포인트 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - UserPoint 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
data class UserPointResponse(
    val id: Long,
    val userId: Long,
    val balance: Long,
    val version: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
}

/**
 * 포인트 이력 정보 응답 DTO - 프레젠테이션 계층
 */
data class PointHistoryResponse(
    val id: Long,
    val userId: Long,
    val amount: Long,
    val transactionType: PointTransactionType,
    val balanceBefore: Long,
    val balanceAfter: Long,
    val orderId: Long?,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun UserPoint.toResponse(): UserPointResponse = UserPointResponse(
            id = this.id,
            userId = this.userId,
            balance = this.balance.value,
            version = this.version,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )

        fun PointHistory.toResponse(): PointHistoryResponse = PointHistoryResponse(
            id = this.id,
            userId = this.userId,
            amount = this.amount,
            transactionType = this.transactionType,
            balanceBefore = this.balanceBefore,
            balanceAfter = this.balanceAfter,
            orderId = this.orderId,
            description = this.description,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}