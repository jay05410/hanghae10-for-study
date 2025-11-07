package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 차감 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 포인트 차감 비즈니스 플로우 수행
 * - 차감 내역 기록 및 차감 후 잔액 관리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 차감 금액 및 잔액 유효성 검증
 * - 포인트 차감 트랜잭션 관리
 * - 차감 전후 상태 및 이력 관리
 */
@Component
class DeductPointUseCase(
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService
) {

    /**
     * 사용자의 포인트를 차감하고 이력을 기록한다
     *
     * @param userId 인증된 사용자 ID
     * @param amount 차감할 포인트 금액 (양수)
     * @param description 차감 사유 및 설명 (선택적)
     * @return 차감 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 차감 금액이 잘못되거나 잔액이 부족한 경우
     * @throws RuntimeException 포인트 차감 처리에 실패한 경우
     */
    @Transactional
    fun execute(userId: Long, amount: Long, description: String? = null): UserPoint {
        val pointAmount = PointAmount.of(amount)

        // 차감 전 잔액 조회
        val userPointBefore = pointService.getUserPoint(userId)
            ?: throw IllegalArgumentException("사용자 포인트 정보가 없습니다: $userId")
        val balanceBefore = userPointBefore.balance

        // 포인트 차감
        val updatedUserPoint = pointService.deductPoint(userId, pointAmount, userId, description)

        // 히스토리 기록
        pointHistoryService.recordDeductHistory(
            userId = userId,
            amount = pointAmount,
            balanceBefore = balanceBefore,
            balanceAfter = updatedUserPoint.balance,
            description = description
        )

        return updatedUserPoint
    }
}