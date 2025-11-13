package io.hhplus.ecommerce.support

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 쿼리 통계 수집기
 *
 * 역할:
 * - P6Spy와 함께 사용하여 쿼리 성능 통계 수집
 * - 느린 쿼리 자동 감지
 * - 실시간 쿼리 분석 리포트 생성
 *
 * 사용법:
 * ```yaml
 * # application.yml에서 활성화
 * query:
 *   statistics:
 *     enabled: true
 * ```
 */
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "query.statistics", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class QueryStatisticsCollector {

    private val log = LoggerFactory.getLogger(javaClass)

    // 쿼리 실행 통계
    private val queryExecutionCount = AtomicLong(0)
    private val totalExecutionTime = AtomicLong(0)
    private val slowQueries = ConcurrentHashMap<String, SlowQueryInfo>()

    // 테이블별 접근 통계
    private val tableAccessCount = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 쿼리 실행 기록
     */
    fun recordQueryExecution(sql: String, executionTimeMs: Long) {
        queryExecutionCount.incrementAndGet()
        totalExecutionTime.addAndGet(executionTimeMs)

        // 느린 쿼리 감지 (100ms 이상)
        if (executionTimeMs > 100) {
            val simplifiedSql = simplifyQuery(sql)
            slowQueries.compute(simplifiedSql) { _, existing ->
                if (existing == null) {
                    SlowQueryInfo(sql, executionTimeMs, 1)
                } else {
                    existing.copy(
                        totalTime = existing.totalTime + executionTimeMs,
                        count = existing.count + 1,
                        maxTime = maxOf(existing.maxTime, executionTimeMs)
                    )
                }
            }
        }

        // 테이블 접근 통계
        extractTableNames(sql).forEach { tableName ->
            tableAccessCount.computeIfAbsent(tableName) { AtomicLong(0) }.incrementAndGet()
        }
    }

    /**
     * 1분마다 통계 출력
     */
    @Scheduled(fixedDelay = 60000)
    fun printStatistics() {
        if (queryExecutionCount.get() == 0L) return

        log.info("")
        log.info("=" .repeat(80))
        log.info("📊 쿼리 성능 통계 (최근 1분)")
        log.info("=" .repeat(80))
        log.info("총 쿼리 수: ${queryExecutionCount.get()}건")
        log.info("평균 실행 시간: ${if (queryExecutionCount.get() > 0) totalExecutionTime.get() / queryExecutionCount.get() else 0}ms")
        log.info("총 실행 시간: ${totalExecutionTime.get()}ms")
        log.info("")

        if (slowQueries.isNotEmpty()) {
            log.info("🐌 느린 쿼리 TOP 10 (100ms 이상):")
            slowQueries.entries
                .sortedByDescending { it.value.totalTime }
                .take(10)
                .forEachIndexed { index, entry ->
                    val avg = entry.value.totalTime / entry.value.count
                    log.info("${index + 1}. [${entry.value.count}회] 평균: ${avg}ms, 최대: ${entry.value.maxTime}ms")
                    log.info("   SQL: ${entry.key.take(100)}")
                }
            log.info("")
        }

        if (tableAccessCount.isNotEmpty()) {
            log.info("📋 테이블 접근 통계:")
            tableAccessCount.entries
                .sortedByDescending { it.value.get() }
                .take(10)
                .forEach { entry ->
                    log.info("   ${entry.key}: ${entry.value.get()}회")
                }
        }

        log.info("=" .repeat(80))
        log.info("")

        // 통계 초기화
        reset()
    }

    /**
     * 통계 초기화
     */
    fun reset() {
        queryExecutionCount.set(0)
        totalExecutionTime.set(0)
        slowQueries.clear()
        tableAccessCount.clear()
    }

    /**
     * 쿼리 단순화 (파라미터 제거)
     */
    private fun simplifyQuery(sql: String): String {
        return sql
            .replace(Regex("= \\d+"), "= ?")
            .replace(Regex("= '[^']*'"), "= ?")
            .replace(Regex("IN \\([^)]+\\)"), "IN (?)")
            .trim()
    }

    /**
     * SQL에서 테이블명 추출
     */
    private fun extractTableNames(sql: String): Set<String> {
        val tables = mutableSetOf<String>()
        val upperSql = sql.uppercase()

        // FROM 절 파싱
        Regex("FROM\\s+(\\w+)").findAll(upperSql).forEach {
            tables.add(it.groupValues[1].lowercase())
        }

        // JOIN 절 파싱
        Regex("JOIN\\s+(\\w+)").findAll(upperSql).forEach {
            tables.add(it.groupValues[1].lowercase())
        }

        // INSERT/UPDATE/DELETE 파싱
        Regex("(INSERT INTO|UPDATE|DELETE FROM)\\s+(\\w+)").findAll(upperSql).forEach {
            tables.add(it.groupValues[2].lowercase())
        }

        return tables
    }

    data class SlowQueryInfo(
        val sql: String,
        val totalTime: Long,
        val count: Long,
        val maxTime: Long = totalTime
    )
}

/**
 * MySQL 성능 분석 유틸리티
 *
 * 역할:
 * - MySQL Performance Schema 활용
 * - 인덱스 사용률 분석
 * - 테이블별 성능 통계
 */
@Component
@Profile("!test")
class MySQLPerformanceAnalyzer(
    private val jdbcTemplate: JdbcTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 인덱스 사용 통계 조회
     */
    fun getIndexUsageStats(schemaName: String = "ecommerce"): List<IndexUsageInfo> {
        return try {
            jdbcTemplate.query(
                """
                SELECT
                    OBJECT_NAME as tableName,
                    INDEX_NAME as indexName,
                    COUNT_READ as readCount,
                    COUNT_WRITE as writeCount,
                    COUNT_FETCH as fetchCount,
                    COUNT_INSERT as insertCount,
                    COUNT_UPDATE as updateCount,
                    COUNT_DELETE as deleteCount
                FROM performance_schema.table_io_waits_summary_by_index_usage
                WHERE OBJECT_SCHEMA = ?
                  AND INDEX_NAME IS NOT NULL
                ORDER BY COUNT_READ DESC
                LIMIT 50
                """.trimIndent()
            ) { rs, _ ->
                IndexUsageInfo(
                    tableName = rs.getString("tableName"),
                    indexName = rs.getString("indexName"),
                    readCount = rs.getLong("readCount"),
                    writeCount = rs.getLong("writeCount"),
                    fetchCount = rs.getLong("fetchCount"),
                    insertCount = rs.getLong("insertCount"),
                    updateCount = rs.getLong("updateCount"),
                    deleteCount = rs.getLong("deleteCount")
                )
            }
        } catch (e: Exception) {
            log.warn("Performance Schema 조회 실패 (Performance Schema가 비활성화되어 있을 수 있습니다)", e)
            emptyList()
        }
    }

    /**
     * 사용되지 않는 인덱스 찾기
     */
    fun findUnusedIndexes(schemaName: String = "ecommerce"): List<String> {
        return try {
            jdbcTemplate.query(
                """
                SELECT
                    CONCAT(OBJECT_NAME, '.', INDEX_NAME) as indexFullName
                FROM performance_schema.table_io_waits_summary_by_index_usage
                WHERE OBJECT_SCHEMA = ?
                  AND INDEX_NAME IS NOT NULL
                  AND INDEX_NAME != 'PRIMARY'
                  AND COUNT_READ = 0
                  AND COUNT_FETCH = 0
                ORDER BY OBJECT_NAME, INDEX_NAME
                """.trimIndent()
            ) { rs, _ ->
                rs.getString("indexFullName")
            }
        } catch (e: Exception) {
            log.warn("Performance Schema 조회 실패", e)
            emptyList()
        }
    }

    /**
     * 테이블별 I/O 통계
     */
    fun getTableIOStats(schemaName: String = "ecommerce"): List<TableIOInfo> {
        return try {
            jdbcTemplate.query(
                """
                SELECT
                    OBJECT_NAME as tableName,
                    COUNT_READ as readCount,
                    COUNT_WRITE as writeCount,
                    COUNT_FETCH as fetchCount,
                    COUNT_INSERT as insertCount,
                    COUNT_UPDATE as updateCount,
                    COUNT_DELETE as deleteCount
                FROM performance_schema.table_io_waits_summary_by_table
                WHERE OBJECT_SCHEMA = ?
                ORDER BY COUNT_READ DESC
                """.trimIndent()
            ) { rs, _ ->
                TableIOInfo(
                    tableName = rs.getString("tableName"),
                    readCount = rs.getLong("readCount"),
                    writeCount = rs.getLong("writeCount"),
                    fetchCount = rs.getLong("fetchCount"),
                    insertCount = rs.getLong("insertCount"),
                    updateCount = rs.getLong("updateCount"),
                    deleteCount = rs.getLong("deleteCount")
                )
            }
        } catch (e: Exception) {
            log.warn("Performance Schema 조회 실패", e)
            emptyList()
        }
    }

    /**
     * 성능 분석 리포트 출력
     */
    fun printPerformanceReport() {
        log.info("")
        log.info("=" .repeat(80))
        log.info("📊 MySQL 성능 분석 리포트")
        log.info("=" .repeat(80))

        // 인덱스 사용 통계
        val indexStats = getIndexUsageStats()
        if (indexStats.isNotEmpty()) {
            log.info("")
            log.info("📋 인덱스 사용 통계 TOP 10:")
            indexStats.take(10).forEach { stat ->
                log.info("   ${stat.tableName}.${stat.indexName}: READ ${stat.readCount}, WRITE ${stat.writeCount}")
            }
        }

        // 사용되지 않는 인덱스
        val unusedIndexes = findUnusedIndexes()
        if (unusedIndexes.isNotEmpty()) {
            log.info("")
            log.info("⚠️  사용되지 않는 인덱스 (삭제 검토):")
            unusedIndexes.forEach { index ->
                log.info("   $index")
            }
        }

        // 테이블별 I/O 통계
        val tableStats = getTableIOStats()
        if (tableStats.isNotEmpty()) {
            log.info("")
            log.info("📊 테이블별 I/O 통계:")
            tableStats.take(10).forEach { stat ->
                log.info("   ${stat.tableName}: READ ${stat.readCount}, WRITE ${stat.writeCount}")
            }
        }

        log.info("=" .repeat(80))
        log.info("")
    }

    data class IndexUsageInfo(
        val tableName: String,
        val indexName: String,
        val readCount: Long,
        val writeCount: Long,
        val fetchCount: Long,
        val insertCount: Long,
        val updateCount: Long,
        val deleteCount: Long
    )

    data class TableIOInfo(
        val tableName: String,
        val readCount: Long,
        val writeCount: Long,
        val fetchCount: Long,
        val insertCount: Long,
        val updateCount: Long,
        val deleteCount: Long
    )
}
