package io.hhplus.ecommerce.point.domain.entity

import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import java.time.LocalDateTime

/**
 * 포인트 거래 이력 도메인 엔티티 (Pure Domain Model)
 *
 * 역할:
 * - 포인트 거래 이력 불변 데이터 표현
 * - JPA 어노테이션 제거로 인프라 의존성 제거
 * - 이력은 생성 후 변경되지 않는 불변 엔티티
 *
 * 주의: 이 클래스는 불변이며 생성 후 변경되지 않습니다.
 */
data class PointHistory(
    val id: Long = 0,
    val userId: Long,
    val amount: Long,
    val transactionType: PointTransactionType,
    val balanceBefore: Long,
    val balanceAfter: Long,
    val orderId: Long? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val deletedAt: LocalDateTime? = null
) {

    fun isDeleted(): Boolean = deletedAt != null
    fun isDeactivated(): Boolean = !isActive

    companion object {
        fun createEarnHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long? = null,
            description: String? = null,
            createdBy: Long
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = amount.value,
                transactionType = PointTransactionType.EARN,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }

        fun createUseHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long? = null,
            description: String? = null,
            createdBy: Long
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = -amount.value, // 사용은 음수로 저장
                transactionType = PointTransactionType.USE,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }

        fun createExpireHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            description: String? = null,
            createdBy: Long
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = -amount.value, // 소멸은 음수로 저장
                transactionType = PointTransactionType.EXPIRE,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = null,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }

        fun createRefundHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long,
            description: String? = null,
            createdBy: Long
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = amount.value, // 환불은 양수로 저장
                transactionType = PointTransactionType.REFUND,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}