package io.hhplus.tdd.point

import io.hhplus.tdd.common.exception.point.PointException
import io.hhplus.tdd.common.util.DateTimeUtil
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.vo.ChargeAmount
import io.hhplus.tdd.point.vo.UseAmount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 포인트 관리 서비스
 *
 * 포인트 조회, 충전, 사용, 내역 조회 등의 비즈니스 로직을 처리.
 */
@Service
class PointService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 특정 사용자의 포인트를 조회.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 포인트 정보
     *
     * 비즈니스 규칙:
     * - 존재하지 않는 사용자의 경우 0포인트로 초기화된 UserPoint 반환
     */
    fun getPoint(userId: Long): UserPoint {
        logger.debug("포인트 조회 요청: userId=$userId")

        val userPoint = userPointTable.selectById(userId)

        logger.debug("포인트 조회 결과: userId=${userPoint.id}, point=${userPoint.point}")

        return userPoint
    }

    /**
     * 특정 사용자의 포인트 충전/사용 내역을 조회.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 포인트 내역 목록 (최신순, 최대 100건)
     *
     * 비즈니스 규칙:
     * - 시간 역순 정렬 (최신순)
     * - 최대 100건 제한
     */
    fun getHistories(userId: Long): List<PointHistory> {
        logger.debug("포인트 내역 조회 요청: userId=$userId")

        val histories = pointHistoryTable.selectAllByUserId(userId)
            .sortedByDescending { it.timeMillis }
            .take(100)

        logger.debug("포인트 내역 조회 결과: userId=$userId, count=${histories.size}")

        return histories
    }

    /**
     * 특정 사용자의 포인트를 충전.
     *
     * @param userId 사용자 ID
     * @param amount 충전 금액
     * @return 충전 후 사용자의 포인트 정보
     * @throws PointException.MinimumChargeAmount 최소 충전 금액 미달 (1,000원 미만)
     * @throws PointException.MaximumChargeAmount 최대 충전 금액 초과 (1,000,000원 초과)
     * @throws PointException.InvalidChargeUnit 충전 단위 불일치 (100원 단위 아님)
     *
     * 비즈니스 규칙:
     * - ChargeAmount Value Object가 validation 담당
     * - 최소 충전 금액: 1,000원 이상
     * - 최대 충전 금액: 1,000,000원 이하
     * - 충전 단위: 100원 단위만 가능
     * - 충전 내역이 PointHistory에 기록됨
     */
    fun charge(userId: Long, amount: Long): UserPoint {
        logger.info("포인트 충전 요청: userId=$userId, amount=$amount")

        // 1. 충전 금액 검증 - Value Object 생성 시 자동 검증
        val chargeAmount = ChargeAmount(amount)

        // 2. 현재 포인트 조회
        val currentPoint = userPointTable.selectById(userId)
        logger.debug("현재 포인트: userId=${currentPoint.id}, point=${currentPoint.point}")

        // 3. 포인트 충전
        val newPoint = currentPoint.point + chargeAmount.value
        val updatedPoint = userPointTable.insertOrUpdate(userId, newPoint)

        // 4. 충전 내역 기록
        pointHistoryTable.insert(
            id = userId,
            amount = chargeAmount.value,
            transactionType = TransactionType.CHARGE,
            updateMillis = updatedPoint.updateMillis
        )

        logger.info("포인트 충전 완료: userId=$userId, 이전=${currentPoint.point}, 이후=$newPoint, 충전액=${chargeAmount.value}")

        return updatedPoint
    }

    /**
     * 특정 사용자의 포인트를 사용.
     *
     * @param userId 사용자 ID
     * @param amount 사용 금액
     * @return 사용 후 사용자의 포인트 정보
     * @throws PointException.InsufficientBalance 잔고 부족
     * @throws PointException.MinimumUseAmount 최소 사용 금액 미달 (100원 미만)
     * @throws PointException.InvalidUseUnit 사용 단위 불일치 (100원 단위 아님)
     * @throws PointException.DailyUseLimitExceeded 일일 사용 한도 초과 (100,000원)
     *
     * 비즈니스 규칙:
     * - UseAmount Value Object가 validation 담당
     * - 최소 사용 금액: 100원 이상
     * - 사용 단위: 100원 단위만 가능
     * - 일일 사용 한도: 100,000원 (당일 00:00 기준)
     * - 잔고 확인: 현재 잔고 >= 사용 금액
     * - 사용 내역이 PointHistory에 기록됨
     */
    fun use(userId: Long, amount: Long): UserPoint {
        logger.info("포인트 사용 요청: userId=$userId, amount=$amount")

        // 1. 사용 금액 검증 - Value Object 생성 시 자동 검증
        val useAmount = UseAmount(amount)

        // 2. 현재 포인트 조회
        val currentPoint = userPointTable.selectById(userId)
        logger.debug("현재 포인트: userId=${currentPoint.id}, point=${currentPoint.point}")

        // 3. 잔고 확인
        if (currentPoint.point < useAmount.value) {
            throw PointException.InsufficientBalance(currentPoint.point, useAmount.value)
        }

        // 4. 일일 사용 한도 확인
        val todayUsed = calculateTodayUsedAmount(userId)
        if (todayUsed + useAmount.value > DAILY_USE_LIMIT) {
            throw PointException.DailyUseLimitExceeded(todayUsed, useAmount.value)
        }

        // 5. 포인트 사용
        val newPoint = currentPoint.point - useAmount.value
        val updatedPoint = userPointTable.insertOrUpdate(userId, newPoint)

        // 6. 사용 내역 기록
        pointHistoryTable.insert(
            id = userId,
            amount = useAmount.value,
            transactionType = TransactionType.USE,
            updateMillis = updatedPoint.updateMillis
        )

        logger.info("포인트 사용 완료: userId=$userId, 이전=${currentPoint.point}, 이후=$newPoint, 사용액=${useAmount.value}")

        return updatedPoint
    }

    /**
     * 오늘 사용한 포인트 총액 계산
     *
     * @param userId 사용자 ID
     * @return 오늘 사용한 포인트 총액
     */
    private fun calculateTodayUsedAmount(userId: Long): Long {
        val allHistories = pointHistoryTable.selectAllByUserId(userId)

        // 오늘 00:00 시각 (DateTimeUtil 사용)
        val todayStart = DateTimeUtil.getTodayStartMillis()

        // 오늘 사용한 내역만 필터링하여 합계 계산
        return allHistories
            .filter { it.type == TransactionType.USE && it.timeMillis >= todayStart }
            .sumOf { it.amount }
    }

    companion object {
        private const val DAILY_USE_LIMIT = 100000L
    }
}