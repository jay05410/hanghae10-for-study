package io.hhplus.ecommerce.payment.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.payment.domain.constant.TransactionType
// import jakarta.persistence.*

// @Entity
// @Table(name = "balance_history")
data class BalanceHistory(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val userId: Long,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    val type: TransactionType,

    // @Column(nullable = false)
    val amount: Long,

    // @Column(nullable = false)
    val balanceAfter: Long,

    // @Column(nullable = false, columnDefinition = "TEXT")
    val description: String
) : ActiveJpaEntity() {
    companion object {
        fun create(
            userId: Long,
            type: TransactionType,
            amount: Long,
            balanceAfter: Long,
            description: String,
            createdBy: Long
        ): BalanceHistory {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }
            require(amount > 0) { "금액은 0보다 커야 합니다" }
            require(balanceAfter >= 0) { "잔액은 0 이상이어야 합니다" }
            require(description.isNotBlank()) { "설명은 필수입니다" }

            return BalanceHistory(
                userId = userId,
                type = type,
                amount = amount,
                balanceAfter = balanceAfter,
                description = description
            )
        }
    }
}