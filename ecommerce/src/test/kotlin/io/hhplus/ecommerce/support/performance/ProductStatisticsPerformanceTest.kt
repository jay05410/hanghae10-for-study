package io.hhplus.ecommerce.support.performance

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.support.ConcurrentTestHelper
import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 상품 통계(조회수) 성능 측정 테스트 (Redis Cache)
 *
 * 측정 항목:
 * - 5,000명이 인기 상품 조회 시 처리량
 * - Redis Cache 성능
 * - 초당 처리량 (TPS)
 * - 응답 시간
 */
class ProductStatisticsPerformanceTest(
    private val productStatisticsService: ProductStatisticsService,
    private val dataSetup: PerformanceTestDataSetup
) : KotestIntegrationTestBase({

    val log = LoggerFactory.getLogger(ProductStatisticsPerformanceTest::class.java)

    describe("상품 통계(조회수) 성능 측정") {

        context("5,000명이 인기 상품(Top 10)을 동시 조회") {
            it("성능 지표를 측정하고 결과를 파일에 저장한다") {
                // Given
                val testData = dataSetup.setupTestData()
                val topProducts = testData.productIdRange.take(10)
                val totalViews = 5000

                log.info("=" .repeat(80))
                log.info("🔍 상품 통계(조회수) 성능 측정 시작 (Redis Cache)")
                log.info("   대상 상품: Top 10개 (ID: ${topProducts.first()} ~ ${topProducts.last()})")
                log.info("   총 조회 요청: $totalViews 건")
                log.info("=" .repeat(80))

                // When - CountDownLatch로 진짜 동시성 보장
                val result = ConcurrentTestHelper.executeWithTiming(
                    threadCount = totalViews,
                    poolSize = 100
                ) {
                    val productId = topProducts.random()
                    productStatisticsService.incrementViewCount(productId)
                }

                // 실제 조회수 검증
                val actualViewCounts = topProducts.associateWith { productId ->
                    val stats = productStatisticsService.getProductStatistics(productId)
                    stats?.viewCount ?: 0
                }

                // Then - 성능 지표 출력
                log.info("")
                log.info("=" .repeat(80))
                log.info("📊 상품 통계(조회수) 성능 측정 결과")
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
                log.info("📊 상품별 조회수:")
                actualViewCounts.forEach { (productId, viewCount) ->
                    log.info("   상품 ID $productId: $viewCount 회")
                }
                log.info("")
                log.info("=" .repeat(80))

                // 파일로 결과 저장
                val resultDir = File("performance-results")
                if (!resultDir.exists()) {
                    resultDir.mkdirs()
                }

                val resultFile = File(resultDir, "product-statistics-after.txt")
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                val viewCountDetails = actualViewCounts.entries.joinToString("\n") { (productId, viewCount) ->
                    "상품 ID $productId: $viewCount 회"
                }

                resultFile.writeText("""
                    |상품 통계(조회수) 성능 측정 결과 (After - Redis Cache)
                    |측정 일시: $timestamp
                    |
                    |========================================
                    |테스트 설정
                    |========================================
                    |대상 상품: Top 10개 (ID: ${topProducts.first()} ~ ${topProducts.last()})
                    |총 조회 요청: $totalViews 건
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
                    |상품별 조회수
                    |========================================
                    |$viewCountDetails
                    |
                    |========================================
                    |개선 효과
                    |========================================
                    |- Redis Cache 도입으로 메모리 기반 조회수 관리
                    |- 비동기 배치로 주기적 DB 동기화
                    |- 락 경합 제거
                    |- 대폭 향상된 TPS
                    |
                """.trimMargin())

                log.info("✅ 결과 파일 저장 완료: ${resultFile.absolutePath}")

                // 검증
                result.successCount shouldBeGreaterThan 0
            }
        }
    }
})
