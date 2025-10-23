package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.hhplus.tdd.point.extension.calculateTodayUsage
import io.hhplus.tdd.point.validator.PointValidator
import io.hhplus.tdd.point.vo.UseAmount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 포인트 사용 서비스
 *
 * 책임:
 *  -  검증 책임 → VO, Validator
 *   - 조회 책임 → QueryService, Table
 *   - 계산 책임 → 간단한 뺄셈
 *   - 저장 책임 → Table
 *
 */
@Service
class PointUseService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable,
    private val pointQueryService: PointQueryService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 포인트 사용
     *
     * @param userId 사용자 ID
     * @param amount 사용 금액
     * @return 사용 후 사용자의 포인트 정보
     * @throws io.hhplus.tdd.common.exception.point.PointException.InsufficientBalance 잔고 부족
     * @throws io.hhplus.tdd.common.exception.point.PointException.MinimumUseAmount 최소 사용 금액 미달
     * @throws io.hhplus.tdd.common.exception.point.PointException.InvalidUseUnit 사용 단위 불일치
     * @throws io.hhplus.tdd.common.exception.point.PointException.DailyUseLimitExceeded 일일 사용 한도 초과
     */
    fun use(userId: Long, amount: Long): UserPoint {
        logger.info("포인트 사용 요청: userId=$userId, amount=$amount")

        // 1. 사용 금액 검증 (VO)
        val useAmount = UseAmount(amount)

        // 2. 현재 포인트 조회
        val currentPoint = userPointTable.selectById(userId)
        logger.debug("현재 포인트: userId=${currentPoint.id}, point=${currentPoint.point}")

        // 3. 잔고 검증 (유틸 사용)
        PointValidator.validateBalance(currentPoint.point, useAmount.value)

        // 4. 일일 한도 검증 (QueryService + 확장함수 사용)
        val todayUsed = pointQueryService.getHistories(userId).calculateTodayUsage()
        PointValidator.validateDailyLimit(todayUsed, useAmount.value)

        // 5. 포인트 계산
        val remainPoint = currentPoint.point - useAmount.value

        // 6. 포인트 업데이트
        val updatedPoint = userPointTable.insertOrUpdate(userId, remainPoint)

        // 7. 사용 내역 기록
        pointHistoryTable.insert(
            userId,
            useAmount.value,
            TransactionType.USE,
            updatedPoint.updateMillis
        )

        logger.info("포인트 사용 완료: userId=$userId, ${currentPoint.point} → $remainPoint")

        return updatedPoint
    }
}