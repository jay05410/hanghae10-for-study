package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 명령 UseCase
 *
 * 역할:
 * - 모든 포인트 변경 작업을 통합 관리
 * - 포인트 충전, 사용, 차감 기능 제공
 * - 히스토리 기록 및 트랜잭션 관리
 *
 * 책임:
 * - 포인트 변경 요청 검증 및 실행
 * - 히스토리 기록 관리
 * - 트랜잭션 무결성 보장
 */
@Component
class PointCommandUseCase(
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService
) {

    /**
     * 사용자에게 포인트를 충전하고 이력을 기록한다
     *
     * @param userId 인증된 사용자 ID
     * @param amount 충전할 포인트 금액 (양수)
     * @param description 충전 사유 및 설명 (선택적)
     * @param orderId 주문 ID (구매 적립인 경우)
     * @return 충전 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 충전 금액이 잘못된 경우
     * @throws RuntimeException 포인트 충전 처리에 실패한 경우
     */
    @Transactional
    fun chargePoint(userId: Long, amount: Long, description: String? = null, orderId: Long? = null): UserPoint {
        val pointAmount = PointAmount.of(amount)

        // 사용자 포인트가 없는 경우 새로 생성
        val existingUserPoint = pointService.getUserPoint(userId)
        if (existingUserPoint == null) {
            pointService.createUserPoint(userId)
        }

        // 충전 전 잔액 조회
        val userPointBefore = pointService.getUserPoint(userId)!!
        val balanceBefore = userPointBefore.balance

        // 포인트 충전
        val updatedUserPoint = pointService.earnPoint(userId, pointAmount, description)

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

    /**
     * 사용자의 포인트를 사용하고 이력을 기록한다
     *
     * @param userId 인증된 사용자 ID
     * @param amount 사용할 포인트 금액 (양수)
     * @param description 사용 사유 및 설명 (선택적)
     * @param orderId 주문 ID (주문 할인인 경우)
     * @return 사용 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 사용 금액이 잘못되거나 잔액이 부족한 경우
     * @throws RuntimeException 포인트 사용 처리에 실패한 경우
     */
    @Transactional
    fun usePoint(userId: Long, amount: Long, description: String? = null, orderId: Long? = null): UserPoint {
        val pointAmount = PointAmount.of(amount)

        // 사용 전 잔액 조회
        val userPointBefore = pointService.getUserPoint(userId)
            ?: throw IllegalArgumentException("사용자 포인트 정보가 없습니다: $userId")
        val balanceBefore = userPointBefore.balance

        // 포인트 사용
        val updatedUserPoint = pointService.usePoint(userId, pointAmount, description)

        // 히스토리 기록
        pointHistoryService.recordUseHistory(
            userId = userId,
            amount = pointAmount,
            balanceBefore = balanceBefore,
            balanceAfter = updatedUserPoint.balance,
            description = description,
            orderId = orderId
        )

        return updatedUserPoint
    }

    /**
     * 포인트를 차감합니다 (히스토리 없이 직접 차감)
     *
     * @param userId 사용자 ID
     * @param amount 차감할 포인트 금액
     * @param deductedBy 차감 요청자 ID
     * @param description 차감 설명
     * @return 차감 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 잔액 부족 시
     * @throws PointException.InvalidAmount 차감 금액이 0 이하인 경우
     */
    @Transactional
    fun deductPoint(userId: Long, amount: PointAmount, deductedBy: Long, description: String? = null): UserPoint {
        return pointService.usePoint(userId, amount, description)
    }

    /**
     * 새 사용자 포인트를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param createdBy 생성 요청자 ID
     * @return 생성된 사용자 포인트 정보
     */
    @Transactional
    fun createUserPoint(userId: Long, createdBy: Long): UserPoint {
        return pointService.createUserPoint(userId)
    }

    /**
     * 포인트를 적립합니다 (히스토리 없이 직접 적립)
     *
     * @param userId 사용자 ID
     * @param amount 적립할 포인트 금액
     * @param earnedBy 적립 요청자 ID
     * @param description 적립 설명
     * @return 적립 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.MaxBalanceExceeded 최대 잔액 초과 시
     */
    @Transactional
    fun earnPoint(userId: Long, amount: PointAmount, earnedBy: Long, description: String? = null): UserPoint {
        return pointService.earnPoint(userId, amount, description)
    }

    /**
     * 포인트를 소멸시킵니다.
     *
     * @param userId 사용자 ID
     * @param amount 소멸할 포인트 금액
     * @return 소멸 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 소멸 가능한 포인트 부족 시
     * @throws PointException.InvalidAmount 소멸 금액이 0 이하인 경우
     */
    @Transactional
    fun expirePoint(userId: Long, amount: PointAmount): UserPoint {
        return pointService.expirePoint(userId, amount)
    }
}