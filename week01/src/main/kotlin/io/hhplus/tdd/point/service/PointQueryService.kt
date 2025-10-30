package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.UserPoint
import io.hhplus.tdd.point.extension.calculateTodayUsage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 포인트 조회 서비스
 *
 * 책임: 포인트 및 내역 조회만
 */
@Service
class PointQueryService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 포인트 조회
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 포인트 정보
     */
    fun getPoint(userId: Long): UserPoint {
        logger.debug("포인트 조회: userId=$userId")
        return userPointTable.selectById(userId)
    }

    /**
     * 포인트 내역 조회 (최신순, 최대 100건)
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 포인트 내역 목록
     */
    fun getHistories(userId: Long): List<PointHistory> {
        logger.debug("포인트 내역 조회: userId=$userId")
        return pointHistoryTable.selectAllByUserId(userId)
            .sortedByDescending { it.timeMillis }
            .take(100)
    }
}