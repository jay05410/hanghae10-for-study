package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Component

/**
 * 포인트 히스토리 UseCase
 *
 * 역할:
 * - 포인트 이력 기록 관련 작업을 통합 관리
 * - 적립/소멸 이력 기록 기능 제공
 *
 * 책임:
 * - 포인트 이력 기록 요청 검증 및 실행
 * - 포인트 히스토리 데이터 무결성 보장
 */
@Component
class PointHistoryUseCase(
    private val pointHistoryService: PointHistoryService
) {

    /**
     * 포인트 적립 이력을 기록합니다.
     *
     * @param userId 사용자 ID
     * @param amount 적립된 포인트 금액
     * @param balanceBefore 적립 전 잔액
     * @param balanceAfter 적립 후 잔액
     * @param createdBy 기록 요청자 ID
     * @param description 적립 설명
     * @param orderId 연관된 주문 ID (선택사항)
     * @return 기록된 포인트 이력 정보
     */
    fun recordEarnHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        createdBy: Long,
        description: String? = null,
        orderId: Long? = null
    ): PointHistory {
        return pointHistoryService.recordEarnHistory(
            userId, amount, balanceBefore, balanceAfter, createdBy, description, orderId
        )
    }

    /**
     * 포인트 소멸 이력을 기록합니다.
     *
     * @param userId 사용자 ID
     * @param amount 소멸된 포인트 금액
     * @param balanceBefore 소멸 전 잔액
     * @param balanceAfter 소멸 후 잔액
     * @param createdBy 기록 요청자 ID
     * @param description 소멸 설명
     * @return 기록된 포인트 이력 정보
     */
    fun recordExpireHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        createdBy: Long,
        description: String? = null
    ): PointHistory {
        return pointHistoryService.recordExpireHistory(
            userId, amount, balanceBefore, balanceAfter, createdBy, description
        )
    }

    /**
     * 사용자의 포인트 이력을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 포인트 이력 목록 (최신순)
     */
    fun getPointHistory(userId: Long): List<PointHistory> {
        return pointHistoryService.getPointHistories(userId)
    }
}