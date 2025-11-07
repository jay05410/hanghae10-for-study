package io.hhplus.ecommerce.common.util

import cn.ipokerface.snowflake.SnowflakeIdGenerator
import org.springframework.stereotype.Component

/**
 * Snowflake ID 생성 유틸리티
 *
 * Twitter Snowflake 알고리즘을 사용하여 분산 환경에서 유니크한 ID 생성
 * - 64bit 구조: 1bit(미사용) + 41bit(timestamp) + 5bit(datacenterId) + 5bit(workerId) + 12bit(sequence)
 * - workerId: 서버별 고유 ID (0-31)
 * - datacenterId: 데이터센터별 고유 ID (0-31)
 */
@Component
class SnowflakeGenerator {

    private val snowflake: SnowflakeIdGenerator

    init {
        // workerId: 서버별 고유 ID (0-31)
        val workerId = System.getenv("WORKER_ID")?.toLongOrNull() ?: 1L
        // datacenterId: 데이터센터별 고유 ID (0-31)
        val datacenterId = System.getenv("DATACENTER_ID")?.toLongOrNull() ?: 1L

        snowflake = SnowflakeIdGenerator(workerId, datacenterId)
    }

    /**
     * 새로운 Snowflake ID 생성
     *
     * @return 64bit Long 타입의 유니크 ID
     */
    fun nextId(): Long = snowflake.nextId()

    /**
     * 접두사가 포함된 고유 번호 생성
     *
     * @param prefix 접두사 enum (타입 안정성 보장)
     * @return 접두사 + 16진수 Snowflake ID (예: "ORDABC123DEF456")
     */
    fun generateNumberWithPrefix(prefix: IdPrefix): String {
        val snowflakeId = nextId()
        return "${prefix.value}${snowflakeId.toString(16).uppercase()}"
    }

    /**
     * Snowflake ID에서 타임스탬프 추출
     *
     * @param snowflakeId Snowflake ID
     * @return 생성 시점의 Unix 타임스탬프 (밀리초)
     */
    fun extractTimestamp(snowflakeId: Long): Long {
        // Snowflake의 타임스탬프는 상위 41bit에 저장됨
        return (snowflakeId shr 22) + EPOCH_OFFSET
    }

    companion object {
        // cn.ipokerface 라이브러리의 기본 epoch (2020-10-01T00:00:00.000Z)
        private const val EPOCH_OFFSET = 1601510400000L
    }
}