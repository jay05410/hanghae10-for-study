package io.hhplus.tdd.point.extension

import io.hhplus.tdd.common.util.DateTimeUtil
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType

/**
 * PointHistory 확장 함수 모음
 *
 * 책임: DB 호출 없는 순수 계산/변환 로직만 포함
 */

/**
 * 오늘 사용한 포인트 총액 계산
 *
 * @return 오늘 사용한 포인트 총액
 */
fun List<PointHistory>.calculateTodayUsage(): Long {
    val todayStart = DateTimeUtil.getTodayStartMillis()

    return this
        .filter { it.type == TransactionType.USE && it.timeMillis >= todayStart }
        .sumOf { it.amount }
}