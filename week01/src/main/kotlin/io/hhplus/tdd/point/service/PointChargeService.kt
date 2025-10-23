package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.hhplus.tdd.point.vo.ChargeAmount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 포인트 충전 서비스
 *
 * 책임: 포인트 충전만
 */
@Service
class PointChargeService(
    private val userPointTable: UserPointTable,
    private val userLockManager: UserLockManager,
    private val transactionLogger: PointTransactionLogger
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 포인트 충전
     *
     * @param userId 사용자 ID
     * @param amount 충전 금액
     * @return 충전 후 사용자의 포인트 정보
     * @throws io.hhplus.tdd.common.exception.point.PointException.MinimumChargeAmount 최소 충전 금액 미달
     * @throws io.hhplus.tdd.common.exception.point.PointException.MaximumChargeAmount 최대 충전 금액 초과
     * @throws io.hhplus.tdd.common.exception.point.PointException.InvalidChargeUnit 충전 단위 불일치
     */
    fun charge(userId: Long, amount: Long): UserPoint {
        logger.info("포인트 충전 요청: userId=$userId, amount=$amount")

        // 1. 충전 금액 검증
        val chargeAmount = ChargeAmount(amount)

        // 2. 사용자별 락 - 동시성 제어
        return userLockManager.executeWithLock(userId) {
            // 3. 현재 포인트 조회
            val currentPoint = userPointTable.selectById(userId)
            logger.debug("현재 포인트: userId=${currentPoint.id}, point=${currentPoint.point}")

            // 4. 포인트 계산
            val remainPoint = currentPoint.point + chargeAmount.value

            // 5. 포인트 업데이트
            val updatedPoint = userPointTable.insertOrUpdate(userId, remainPoint)

            // 6. 충전 내역 기록
            transactionLogger.logTransaction(
                userId,
                chargeAmount.value,
                TransactionType.CHARGE,
                updatedPoint.updateMillis
            )

            logger.info("포인트 충전 완료: userId=$userId, ${currentPoint.point} → $remainPoint")

            updatedPoint
        }
    }
}