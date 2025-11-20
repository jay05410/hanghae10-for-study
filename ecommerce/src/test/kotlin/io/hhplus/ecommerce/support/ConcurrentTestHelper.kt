package io.hhplus.ecommerce.support

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 동시성 테스트 헬퍼
 *
 * CountDownLatch를 사용하여 진짜 "동시 시작"을 보장합니다.
 */
class ConcurrentTestHelper {

    companion object {
        /**
         * 동시성 테스트를 실행합니다.
         *
         * @param threadCount 동시 실행할 스레드 수
         * @param poolSize 스레드 풀 크기 (기본값: threadCount)
         * @param action 각 스레드가 실행할 작업
         * @return 테스트 결과
         */
        fun execute(
            threadCount: Int = 100,
            poolSize: Int = threadCount,
            action: () -> Unit
        ): ConcurrentTestResult {
            val executor = Executors.newFixedThreadPool(poolSize)
            val startLatch = CountDownLatch(1)  // 시작 신호
            val endLatch = CountDownLatch(threadCount)  // 완료 대기
            val errors = ConcurrentLinkedQueue<Throwable>()
            val successCount = AtomicInteger(0)
            val startTime = System.currentTimeMillis()

            repeat(threadCount) {
                executor.submit {
                    try {
                        startLatch.await()  // 모든 스레드가 여기서 대기
                        action()
                        successCount.incrementAndGet()
                    } catch (e: Throwable) {
                        errors.add(e)
                    } finally {
                        endLatch.countDown()
                    }
                }
            }

            // 모든 스레드가 준비되면 동시에 시작
            startLatch.countDown()

            // 모든 스레드가 완료될 때까지 대기
            endLatch.await()
            executor.shutdown()

            val duration = System.currentTimeMillis() - startTime

            return ConcurrentTestResult(
                totalRequests = threadCount,
                successCount = successCount.get(),
                errorCount = errors.size,
                errors = errors.toList(),
                durationMs = duration
            )
        }

        /**
         * 동시성 테스트를 실행하고 응답 시간을 측정합니다.
         *
         * @param threadCount 동시 실행할 스레드 수
         * @param poolSize 스레드 풀 크기
         * @param action 각 스레드가 실행할 작업
         * @return 상세 성능 결과
         */
        fun executeWithTiming(
            threadCount: Int = 100,
            poolSize: Int = threadCount,
            action: () -> Unit
        ): DetailedConcurrentTestResult {
            val executor = Executors.newFixedThreadPool(poolSize)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val errors = ConcurrentLinkedQueue<Throwable>()
            val successCount = AtomicInteger(0)
            val responseTimes = ConcurrentLinkedQueue<Long>()
            val overallStartTime = System.currentTimeMillis()

            repeat(threadCount) {
                executor.submit {
                    try {
                        startLatch.await()
                        val requestStartTime = System.currentTimeMillis()
                        action()
                        val requestDuration = System.currentTimeMillis() - requestStartTime
                        responseTimes.add(requestDuration)
                        successCount.incrementAndGet()
                    } catch (e: Throwable) {
                        errors.add(e)
                    } finally {
                        endLatch.countDown()
                    }
                }
            }

            startLatch.countDown()
            endLatch.await()
            executor.shutdown()

            val totalDuration = System.currentTimeMillis() - overallStartTime

            return DetailedConcurrentTestResult(
                totalRequests = threadCount,
                successCount = successCount.get(),
                errorCount = errors.size,
                errors = errors.toList(),
                responseTimes = responseTimes.toList(),
                totalDurationMs = totalDuration
            )
        }
    }
}

data class ConcurrentTestResult(
    val totalRequests: Int,
    val successCount: Int,
    val errorCount: Int,
    val errors: List<Throwable>,
    val durationMs: Long
) {
    val failCount: Int
        get() = errorCount

    val successRate: Double
        get() = successCount * 100.0 / totalRequests

    val tps: Double
        get() = if (durationMs > 0) successCount * 1000.0 / durationMs else 0.0

    fun printSummary() {
        println("""
            ===== Concurrent Test Result =====
            Total Requests: $totalRequests
            Success: $successCount
            Errors: $errorCount
            Duration: ${durationMs}ms
            Success Rate: ${"%.2f".format(successRate)}%
            TPS: ${"%.2f".format(tps)}
            ==================================
        """.trimIndent())
    }
}

data class DetailedConcurrentTestResult(
    val totalRequests: Int,
    val successCount: Int,
    val errorCount: Int,
    val errors: List<Throwable>,
    val responseTimes: List<Long>,
    val totalDurationMs: Long
) {
    val failCount: Int
        get() = errorCount

    val successRate: Double
        get() = successCount * 100.0 / totalRequests

    val tps: Double
        get() = if (totalDurationMs > 0) successCount * 1000.0 / totalDurationMs else 0.0

    val avgResponseTime: Double
        get() = if (responseTimes.isNotEmpty()) responseTimes.average() else 0.0

    val minResponseTime: Long
        get() = responseTimes.minOrNull() ?: 0L

    val maxResponseTime: Long
        get() = responseTimes.maxOrNull() ?: 0L

    val medianResponseTime: Long
        get() {
            if (responseTimes.isEmpty()) return 0L
            val sorted = responseTimes.sorted()
            return sorted[sorted.size / 2]
        }

    val p95ResponseTime: Long
        get() {
            if (responseTimes.isEmpty()) return 0L
            val sorted = responseTimes.sorted()
            return sorted[(sorted.size * 0.95).toInt()]
        }

    val p99ResponseTime: Long
        get() {
            if (responseTimes.isEmpty()) return 0L
            val sorted = responseTimes.sorted()
            return sorted[(sorted.size * 0.99).toInt()]
        }

    fun printDetailedSummary() {
        println("""
            ===== Detailed Concurrent Test Result =====
            Total Requests: $totalRequests
            Success: $successCount (${String.format("%.2f", successRate)}%)
            Errors: $errorCount

            Response Time (ms):
              Avg: ${String.format("%.2f", avgResponseTime)}
              Median: $medianResponseTime
              P95: $p95ResponseTime
              P99: $p99ResponseTime
              Min: $minResponseTime
              Max: $maxResponseTime

            Performance:
              Total Duration: ${totalDurationMs}ms
              TPS: ${String.format("%.2f", tps)}
            ===========================================
        """.trimIndent())
    }
}
