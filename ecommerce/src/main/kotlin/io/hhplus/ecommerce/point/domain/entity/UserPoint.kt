package io.hhplus.ecommerce.point.domain.entity

import io.hhplus.ecommerce.point.exception.PointException
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.point.domain.constant.PointAccountStatus

/**
 * 사용자 포인트 도메인 엔티티 (Pure Domain Model)
 *
 * 역할:
 * - 포인트 비즈니스 로직 캡슐화
 * - 가변 구조로 도메인 무결성 보장
 * - JPA 어노테이션 제거로 인프라 의존성 제거
 *
 * 포인트는 구매 적립 혜택 시스템:
 * - 상품 구매 시 일정 % 자동 적립
 * - 다음 구매 시 포인트로 할인 가능
 * - 일정 기간 후 자동 소멸
 *
 * 주의: 모든 상태 변경 메서드는 직접 필드를 수정하고 이전 잔액을 반환합니다.
 */
data class UserPoint(
    val id: Long = 0,
    val userId: Long,
    var balance: Balance = Balance.zero(),
    var version: Int = 0,
    var status: PointAccountStatus = PointAccountStatus.ACTIVE
) {

    /**
     * 활성 상태 확인
     *
     * @return 활성 여부
     */
    fun isActive(): Boolean = status == PointAccountStatus.ACTIVE

    /**
     * 포인트 적립 (구매 시 자동 적립)
     *
     * @param amount 적립 금액
     * @return 적립 전 잔액
     * @throws PointException.MaxBalanceExceeded 최대 잔액 초과 시
     */
    fun earn(amount: PointAmount): Balance {
        val oldBalance = this.balance
        this.balance = this.balance + amount.value
        return oldBalance
    }

    /**
     * 포인트 사용 (할인 적용)
     *
     * @param amount 사용 금액
     * @return 사용 전 잔액
     * @throws PointException.InvalidAmount 사용 금액이 0 이하인 경우
     * @throws PointException.InsufficientBalance 잔액 부족 시
     */
    fun use(amount: PointAmount): Balance {
        if (amount.value <= 0) {
            throw PointException.InvalidAmount(amount.value)
        }
        if (!this.balance.canAfford(amount.value)) {
            throw PointException.InsufficientBalance(this.balance.value, amount.value)
        }

        val oldBalance = this.balance
        this.balance = this.balance - amount.value
        return oldBalance
    }

    /**
     * 포인트 소멸 (만료 처리)
     *
     * @param amount 소멸 금액
     * @return 소멸 전 잔액
     * @throws PointException.InvalidAmount 소멸 금액이 0 이하인 경우
     * @throws PointException.InsufficientBalance 소멸 가능한 포인트 부족 시
     */
    fun expire(amount: PointAmount): Balance {
        if (amount.value <= 0) {
            throw PointException.InvalidAmount(amount.value)
        }
        if (!this.balance.canAfford(amount.value)) {
            throw PointException.InsufficientBalance(this.balance.value, amount.value)
        }

        val oldBalance = this.balance
        this.balance = this.balance - amount.value
        return oldBalance
    }


    /**
     * 포인트 계정 비활성화
     */
    fun deactivate() {
        this.status = PointAccountStatus.INACTIVE
    }

    /**
     * 포인트 계정 활성화
     */
    fun activate() {
        this.status = PointAccountStatus.ACTIVE
    }

    /**
     * 포인트 계정 일시정지
     */
    fun suspend() {
        this.status = PointAccountStatus.SUSPENDED
    }


    companion object {
        fun create(userId: Long): UserPoint {
            return UserPoint(
                userId = userId,
                balance = Balance.zero()
            )
        }
    }
}