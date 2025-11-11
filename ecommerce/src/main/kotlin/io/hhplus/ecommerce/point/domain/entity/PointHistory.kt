package io.hhplus.ecommerce.point.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.point.domain.vo.PointAmount
// import jakarta.persistence.*

// @Entity
// @Table(name = "point_history")
class PointHistory(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val userId: Long,

    // @Column(nullable = false)
    val amount: Long,

    // @Enumerated(EnumType.STRING)
    // @Column(nullable = false)
    val transactionType: PointTransactionType,

    // @Column(nullable = false)
    val balanceBefore: Long,

    // @Column(nullable = false)
    val balanceAfter: Long,

    // @Column(nullable = true)
    val orderId: Long? = null,

    // @Column(nullable = true)
    val description: String? = null
) : ActiveJpaEntity() {

    companion object {
        fun createEarnHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long? = null,
            description: String? = null
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = amount.value,
                transactionType = PointTransactionType.EARN,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description
            )
        }

        fun createUseHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long? = null,
            description: String? = null
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = -amount.value, // 사용은 음수로 저장
                transactionType = PointTransactionType.USE,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description
            )
        }

        fun createExpireHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            description: String? = null
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = -amount.value, // 소멸은 음수로 저장
                transactionType = PointTransactionType.EXPIRE,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = null,
                description = description
            )
        }

        fun createRefundHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long,
            description: String? = null
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = amount.value, // 환불은 양수로 저장
                transactionType = PointTransactionType.REFUND,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description
            )
        }
    }
}