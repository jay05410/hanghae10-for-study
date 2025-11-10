package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 적립 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 포인트 적립 비즈니스 플로우 수행 (구매 시 자동 적립)
 * - 적립 내역 기록 및 잔액 관리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 적립 금액 유효성 검증
 * - 포인트 적립 트랜잭션 관리
 * - 적립 전후 상태 및 이력 관리
 */
@Component
class ChargePointUseCase(
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService
) {

    /**
     * 사용자에게 포인트를 적립하고 이력을 기록한다
     *
     * @param userId 인증된 사용자 ID
     * @param amount 적립할 포인트 금액 (양수)
     * @param description 적립 사유 및 설명 (선택적)
     * @param orderId 주문 ID (구매 적립인 경우)
     * @return 적립 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 적립 금액이 잘못된 경우
     * @throws RuntimeException 포인트 적립 처리에 실패한 경우
     */
    @Transactional
    fun execute(userId: Long, amount: Long, description: String? = null, orderId: Long? = null): UserPoint {
        val pointAmount = PointAmount.of(amount)

        // 사용자 포인트가 없는 경우 새로 생성
        val existingUserPoint = pointService.getUserPoint(userId)
        if (existingUserPoint == null) {
            pointService.createUserPoint(userId, userId)
        }

        // 적립 전 잔액 조회
        val userPointBefore = pointService.getUserPoint(userId)!!
        val balanceBefore = userPointBefore.balance

        // 포인트 적립
        val updatedUserPoint = pointService.earnPoint(userId, pointAmount, userId, description)

        // 히스토리 기록
        pointHistoryService.recordEarnHistory(
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