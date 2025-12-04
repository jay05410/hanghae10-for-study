package io.hhplus.ecommerce.point.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Component

/**
 * 포인트 사용 유스케이스 - Application Layer
 *
 * 역할:
 * - 포인트 사용 비즈니스 흐름 오케스트레이션
 * - 분산락을 통한 동시 사용 방지
 * - 트랜잭션 경계 관리
 *
 * 책임:
 * - 사용 흐름 조정 (검증 → 사용 → 이력 기록)
 * - PointDomainService 협력 (도메인 로직)
 *
 * 동시성 제어:
 * - userId 기반 분산락으로 동시 사용 방지
 * - 분산 트랜잭션으로 데이터 정합성 보장
 */
@Component
class UsePointUseCase(
    private val pointDomainService: PointDomainService
) {

    /**
     * 사용자의 포인트를 사용하고 이력을 기록
     *
     * 동시성 제어:
     * - @DistributedLock(Order 0): 분산락으로 동일 사용자의 동시 사용 방지
     * - @DistributedTransaction(Order 1): 락 내부에서 트랜잭션 실행
     *
     * @param userId 인증된 사용자 ID
     * @param amount 사용할 포인트 금액 (양수)
     * @param description 사용 사유 및 설명 (선택적)
     * @param orderId 주문 ID (주문 할인인 경우)
     * @return 사용 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 사용 금액이 잘못되거나 잔액이 부족한 경우
     */
    @DistributedLock(key = DistributedLockKeys.Point.USE, waitTime = 120L, leaseTime = 60L)
    @DistributedTransaction
    fun execute(userId: Long, amount: Long, description: String? = null, orderId: Long? = null): UserPoint {
        val pointAmount = PointAmount.of(amount)

        // 1. 사용 전 잔액 조회
        val userPointBefore = pointDomainService.getUserPointOrThrow(userId)
        val balanceBefore = userPointBefore.balance

        // 2. 포인트 사용
        val updatedUserPoint = pointDomainService.usePoint(userId, pointAmount)

        // 3. 히스토리 기록
        pointDomainService.recordUseHistory(
            userId = userId,
            amount = pointAmount,
            balanceBefore = balanceBefore,
            balanceAfter = updatedUserPoint.balance,
            description = description,
            orderId = orderId
        )

        return updatedUserPoint
    }
}
