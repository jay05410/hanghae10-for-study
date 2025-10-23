package io.hhplus.tdd.point.validator

import io.hhplus.tdd.common.exception.point.PointException

/**
 * 포인트 검증 유틸리티
 *
 * 책임: DB 호출 없는 순수 검증 로직만 포함
 */
object PointValidator {

    /**
     * 잔고가 충분한지 검증
     *
     * @param currentPoint 현재 보유 포인트
     * @param useAmount 사용하려는 포인트
     * @throws PointException.InsufficientBalance 잔고 부족 시
     */
    fun validateBalance(currentPoint: Long, useAmount: Long) {
        if (currentPoint < useAmount) throw PointException.InsufficientBalance(currentPoint, useAmount)
    }

    /**
     * 일일 사용 한도 검증
     *
     * @param todayUsed 오늘 이미 사용한 포인트
     * @param useAmount 추가로 사용하려는 포인트
     * @throws PointException.DailyUseLimitExceeded 일일 한도 초과 시
     */
    fun validateDailyLimit(todayUsed: Long, useAmount: Long) {
        if (todayUsed + useAmount > DAILY_USE_LIMIT) {
            throw PointException.DailyUseLimitExceeded(todayUsed, useAmount)
        }
    }

    private const val DAILY_USE_LIMIT = 100000L
}