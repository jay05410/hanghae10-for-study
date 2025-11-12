package io.hhplus.ecommerce.point.application

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 이력 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 포인트 변동 이력 관리 전담
 * - 포인트 거래 기록 및 추적
 * - 포인트 사용 패턴 분석 데이터 제공
 *
 * 책임:
 * - 포인트 적립/사용/소멸 이력 기록
 * - 사용자별 포인트 이력 조회
 * - 포인트 사용 패턴 제공
 */
@Service
class PointHistoryService(
    private val pointHistoryRepository: PointHistoryRepository
) {

    @Transactional
    fun recordEarnHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        createdBy: Long,
        description: String? = null,
        orderId: Long? = null
    ): PointHistory {
        val history = PointHistory.createEarnHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore.value,
            balanceAfter = balanceAfter.value,
            orderId = orderId,
            description = description,
            createdBy = createdBy
        )
        return pointHistoryRepository.save(history)
    }

    @Transactional
    fun recordUseHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        createdBy: Long,
        description: String? = null,
        orderId: Long? = null
    ): PointHistory {
        val history = PointHistory.createUseHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore.value,
            balanceAfter = balanceAfter.value,
            orderId = orderId,
            description = description,
            createdBy = createdBy
        )
        return pointHistoryRepository.save(history)
    }

    @Transactional
    fun recordExpireHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        createdBy: Long,
        description: String? = null
    ): PointHistory {
        val history = PointHistory.createExpireHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore.value,
            balanceAfter = balanceAfter.value,
            description = description,
            createdBy = createdBy
        )
        return pointHistoryRepository.save(history)
    }

    @Transactional(readOnly = true)
    fun getPointHistories(userId: Long): List<PointHistory> {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
}