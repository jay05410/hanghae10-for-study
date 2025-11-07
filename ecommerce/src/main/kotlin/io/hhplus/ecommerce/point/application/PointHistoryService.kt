package io.hhplus.ecommerce.point.application

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
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
 * - 포인트 충전/차감 이력 기록
 * - 사용자별 포인트 이력 조회
 * - 포인트 사용 내역 제공
 */
@Service
class PointHistoryService(
    private val pointHistoryRepository: PointHistoryRepository
) {

    @Transactional
    fun recordChargeHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Long,
        balanceAfter: Long,
        description: String? = null
    ): PointHistory {
        val history = PointHistory.createChargeHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description
        )
        return pointHistoryRepository.save(history)
    }

    @Transactional
    fun recordDeductHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Long,
        balanceAfter: Long,
        description: String? = null
    ): PointHistory {
        val history = PointHistory.createDeductHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description
        )
        return pointHistoryRepository.save(history)
    }

    @Transactional(readOnly = true)
    fun getPointHistories(userId: Long): List<PointHistory> {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
}