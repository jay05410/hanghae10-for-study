package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.point.TransactionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 포인트 거래 내역 기록
 *
 * 책임: 포인트 충전/사용 내역을 PointHistoryTable에 기록
 */
@Component
class PointTransactionLogger(
    private val pointHistoryTable: PointHistoryTable
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 포인트 거래 내역을 기록
     *
     * @param userId 사용자 ID
     * @param amount 거래 금액
     * @param transactionType 거래 유형 (CHARGE/USE)
     * @param updateMillis 거래 시점
     */
    fun logTransaction(
        userId: Long,
        amount: Long,
        transactionType: TransactionType,
        updateMillis: Long
    ) {
        logger.debug("거래 내역 기록: userId={}, amount={}, type={}", userId, amount, transactionType)

        pointHistoryTable.insert(userId, amount, transactionType, updateMillis)

        logger.debug("거래 내역 기록 완료: userId=$userId")
    }
}