package io.hhplus.ecommerce.domain.user.entity

import io.hhplus.ecommerce.domain.payment.validator.PaymentValidator
import io.hhplus.ecommerce.domain.payment.vo.ChargeAmount
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_balance")
class UserBalance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val userId: Long,

    @Column(nullable = false)
    var balance: Long = 0,

    @Version
    var version: Int = 0,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedBy: Long
) {
    fun charge(chargeAmount: ChargeAmount, chargedBy: Long): Long {
        val oldBalance = this.balance
        this.balance += chargeAmount.value
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = chargedBy

        return oldBalance
    }

    fun deduct(amount: Long, deductedBy: Long): Long {
        // 기본 입력값 검증: 음수 차감 방지
        require(amount > 0) { "차감 금액은 0보다 커야 합니다: $amount" }
        // 비즈니스 로직 검증: 잔고 충분 여부 확인
        PaymentValidator.validateBalance(this.balance, amount)

        val oldBalance = this.balance
        this.balance -= amount
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = deductedBy

        return oldBalance
    }

    companion object {

        fun create(userId: Long, createdBy: Long): UserBalance {
            return UserBalance(
                userId = userId,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}