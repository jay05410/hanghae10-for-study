package io.hhplus.ecommerce.point.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.point.domain.vo.PointAmount
// import jakarta.persistence.*

// @Entity
// @Table(name = "user_point")
class UserPoint(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true)
    val userId: Long,

    // @Column(nullable = false)
    var balance: Long = 0,

    // @Version
    var version: Int = 0
) : ActiveJpaEntity() {

    fun charge(amount: PointAmount, chargedBy: Long): Long {
        val oldBalance = this.balance
        this.balance += amount.value
        return oldBalance
    }

    fun deduct(amount: PointAmount, deductedBy: Long): Long {
        require(amount.value > 0) { "차감 금액은 0보다 커야 합니다: ${amount.value}" }
        require(this.balance >= amount.value) { "잔고가 부족합니다. 현재 잔고: $balance, 차감 시도 금액: ${amount.value}" }

        val oldBalance = this.balance
        this.balance -= amount.value
        return oldBalance
    }

    companion object {
        fun create(userId: Long, createdBy: Long): UserPoint {
            return UserPoint(userId = userId)
        }
    }
}