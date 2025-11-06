package io.hhplus.ecommerce.domain.payment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "balance_history")
class BalanceHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val balanceBefore: Long,

    @Column(nullable = false)
    val balanceAfter: Long,

    @Column(nullable = false)
    val relatedId: Long? = null,

    @Column(nullable = false, length = 50)
    val relatedType: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedBy: Long
) {
    companion object {
        fun createChargeHistory(
            userId: Long,
            amount: Long,
            balanceBefore: Long,
            balanceAfter: Long,
            description: String = "포인트 충전",
            createdBy: Long
        ): BalanceHistory {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(amount > 0) { "충전 금액은 0보다 커야 합니다" }
            require(balanceAfter >= balanceBefore) { "충전 후 잔액이 올바르지 않습니다" }

            return BalanceHistory(
                userId = userId,
                transactionType = TransactionType.CHARGE,
                amount = amount,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }

        fun createDeductHistory(
            userId: Long,
            amount: Long,
            balanceBefore: Long,
            balanceAfter: Long,
            relatedId: Long? = null,
            relatedType: String? = null,
            description: String = "포인트 사용",
            createdBy: Long
        ): BalanceHistory {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(amount > 0) { "사용 금액은 0보다 커야 합니다" }
            require(balanceAfter <= balanceBefore) { "사용 후 잔액이 올바르지 않습니다" }

            return BalanceHistory(
                userId = userId,
                transactionType = TransactionType.DEDUCT,
                amount = amount,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                relatedId = relatedId,
                relatedType = relatedType,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}

enum class TransactionType {
    CHARGE, DEDUCT
}