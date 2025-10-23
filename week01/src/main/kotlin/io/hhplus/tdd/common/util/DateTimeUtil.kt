package io.hhplus.tdd.common.util

import java.time.LocalDate
import java.time.ZoneId

/**
 * 날짜/시간 관련 유틸리티
 */
object DateTimeUtil {

    /**
     * 오늘 00:00:00의 Epoch Milliseconds 반환
     *
     * @return 오늘 자정의 타임스탬프 (밀리초)
     */
    fun getTodayStartMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * 주어진 타임스탬프가 오늘인지 확인
     *
     * @param timeMillis 확인할 타임스탬프 (밀리초)
     * @return 오늘이면 true, 아니면 false
     */
    fun isToday(timeMillis: Long): Boolean {
        val todayStart = getTodayStartMillis()
        val tomorrowStart = getTomorrowStartMillis()
        return timeMillis in todayStart until tomorrowStart
    }

    /**
     * 내일 00:00:00의 Epoch Milliseconds 반환
     *
     * @return 내일 자정의 타임스탬프 (밀리초)
     */
    fun getTomorrowStartMillis(): Long {
        return LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * 현재 시각의 Epoch Milliseconds 반환
     *
     * @return 현재 타임스탬프 (밀리초)
     */
    fun getCurrentMillis(): Long {
        return System.currentTimeMillis()
    }
}
