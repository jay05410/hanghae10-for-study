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
        fun createChargeHistory(
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
                transactionType = PointTransactionType.CHARGE,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description
            )
        }

        fun createDeductHistory(
            userId: Long,
            amount: PointAmount,
            balanceBefore: Long,
            balanceAfter: Long,
            orderId: Long? = null,
            description: String? = null
        ): PointHistory {
            return PointHistory(
                userId = userId,
                amount = -amount.value, // 차감은 음수로 저장
                transactionType = PointTransactionType.DEDUCT,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                orderId = orderId,
                description = description
            )
        }
    }
}