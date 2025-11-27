package io.hhplus.ecommerce.support.performance

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.support.ConcurrentTestHelper
import io.hhplus.ecommerce.coupon.usecase.CouponUseCase
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.kotest.matchers.ints.shouldBeGreaterThan
import mu.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

/**
 * 쿠폰 발급 성능 측정 테스트 (Queue 기반)
 *
 * 측정 항목:
 * - 2,000명 동시 요청 시 Queue 등록 성능
 * - 응답 시간 (평균, 중앙값, P95, P99, 최대)
 * - 초당 처리량 (TPS)
 * - 성공/실패 비율
 */
class CouponIssuancePerformanceTest(
    private val couponUseCase: CouponUseCase,
    private val dataSetup: PerformanceTestDataSetup
) : KotestIntegrationTestBase({

    val log = KotlinLogging.logger {}

    describe("쿠폰 발급 성능 측정") {

        context("2,000명이 100개 한정 쿠폰에 동시 발급 시도") {
            it("성능 지표를 측정하고 결과를 파일에 저장한다") {
                // Given
                val testData = dataSetup.setupTestData()
                val couponId = testData.couponIdRange.first
                val totalUsers = 2000
                val counter = AtomicInteger(0)

                log.info("=" .repeat(80))
                log.info("🔍 쿠폰 발급 성능 측정 시작 (Redis Queue)")
                log.info("   대상 쿠폰 ID: $couponId (100개 한정)")
                log.info("   동시 요청 사용자 수: $totalUsers 명")
                log.info("=" .repeat(80))

                // When - CountDownLatch로 진짜 동시성 보장
                val result = ConcurrentTestHelper.executeWithTiming(
                    threadCount = totalUsers,
                    poolSize = 100
                ) {
                    val index = counter.incrementAndGet()
                    val userId = testData.userIdRange.first + index - 1
                    couponUseCase.issueCoupon(
                        userId = userId,
                        request = IssueCouponRequest(couponId = couponId)
                    )
                }

                // Then - 성능 지표 출력
                log.info("")
                log.info("=" .repeat(80))
                log.info("📊 쿠폰 발급 성능 측정 결과")
                log.info("=" .repeat(80))
                log.info("")
                log.info("📈 요청 처리 결과:")
                log.info("   총 요청: ${result.totalRequests} 건")
                log.info("   성공: ${result.successCount} 건 (${String.format("%.2f", result.successRate)}%)")
                log.info("   실패: ${result.errorCount} 건")
                log.info("")
                log.info("⏱️  응답 시간 (ms):")
                log.info("   평균: ${String.format("%.2f", result.avgResponseTime)} ms")
                log.info("   중앙값: ${result.medianResponseTime} ms")
                log.info("   P95: ${result.p95ResponseTime} ms")
                log.info("   P99: ${result.p99ResponseTime} ms")
                log.info("   최소: ${result.minResponseTime} ms")
                log.info("   최대: ${result.maxResponseTime} ms")
                log.info("")
                log.info("🚀 처리량:")
                log.info("   총 소요 시간: ${result.totalDurationMs} ms (${String.format("%.2f", result.totalDurationMs / 1000.0)} 초)")
                log.info("   TPS: ${String.format("%.2f", result.tps)} requests/sec")
                log.info("")
                log.info("=" .repeat(80))

                // 파일로 결과 저장
                val resultDir = File("performance-results")
                if (!resultDir.exists()) {
                    resultDir.mkdirs()
                }

                val resultFile = File(resultDir, "coupon-issuance-after.txt")
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                resultFile.writeText("""
                    |쿠폰 발급 성능 측정 결과 (After - Redis Queue)
                    |측정 일시: $timestamp
                    |
                    |========================================
                    |테스트 설정
                    |========================================
                    |쿠폰 ID: $couponId
                    |쿠폰 수량: 100개
                    |동시 요청 사용자 수: $totalUsers 명
                    |동시성 보장: CountDownLatch 사용
                    |
                    |========================================
                    |요청 처리 결과
                    |========================================
                    |총 요청: ${result.totalRequests} 건
                    |성공: ${result.successCount} 건 (${String.format("%.2f", result.successRate)}%)
                    |실패: ${result.errorCount} 건
                    |
                    |========================================
                    |응답 시간 (ms)
                    |========================================
                    |평균: ${String.format("%.2f", result.avgResponseTime)} ms
                    |중앙값: ${result.medianResponseTime} ms
                    |P95: ${result.p95ResponseTime} ms
                    |P99: ${result.p99ResponseTime} ms
                    |최소: ${result.minResponseTime} ms
                    |최대: ${result.maxResponseTime} ms
                    |
                    |========================================
                    |처리량
                    |========================================
                    |총 소요 시간: ${result.totalDurationMs} ms (${String.format("%.2f", result.totalDurationMs / 1000.0)} 초)
                    |TPS: ${String.format("%.2f", result.tps)} requests/sec
                    |
                    |========================================
                    |개선 효과
                    |========================================
                    |- Redis Queue 도입으로 즉시 대기번호 응답
                    |- 타임아웃 문제 해결
                    |- 사용자 경험 향상 (대기번호 확인 가능)
                    |
                """.trimMargin())

                log.info("✅ 결과 파일 저장 완료: ${resultFile.absolutePath}")

                // 검증
                result.successCount shouldBeGreaterThan 0
            }
        }
    }
})
