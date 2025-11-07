package io.hhplus.ecommerce.payment.domain.repository

import io.hhplus.ecommerce.payment.domain.constant.TransactionType
import io.hhplus.ecommerce.payment.domain.entity.BalanceHistory

interface BalanceHistoryRepository {
    fun save(balanceHistory: BalanceHistory): BalanceHistory
    fun findById(id: Long): BalanceHistory?
    fun findByUserId(userId: Long): List<BalanceHistory>
    fun findByUserIdAndType(userId: Long, type: TransactionType): List<BalanceHistory>
    fun delete(balanceHistory: BalanceHistory)
}