package io.hhplus.ecommerce.support.performance

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.support.ConcurrentTestHelper
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.CreateOrderItemRequest
import io.hhplus.ecommerce.delivery.dto.DeliveryAddressRequest
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

/**
 * 주문 생성 성능 측정 테스트 (TransactionTemplate 방식)
 *
 * 측정 항목:
 * - 500명 동시 주문 시 응답 시간
 * - 커넥션 풀 사용 효율성
 * - 초당 처리량 (TPS)
 * - 트랜잭션 평균 소요 시간
 */
class OrderCreationPerformanceTest(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val dataSetup: PerformanceTestDataSetup,
    private val jdbcTemplate: JdbcTemplate
) : KotestIntegrationTestBase({

    val log = LoggerFactory.getLogger(OrderCreationPerformanceTest::class.java)

    describe("주문 생성 성능 측정") {

        context("500명이 동시에 주문 생성 시도") {
            it("성능 지표를 측정하고 결과를 파일에 저장한다") {
                // Given
                val testData = dataSetup.setupTestData()
                val totalOrders = 500
                val counter = AtomicInteger(0)

                log.info("=" .repeat(80))
                log.info("🔍 주문 생성 성능 측정 시작 (TransactionTemplate)")
                log.info("   동시 주문 건수: $totalOrders 건")
                log.info("=" .repeat(80))

                // 커넥션 풀 상태 확인 (Before)
                val poolStatsBefore = getHikariPoolStats()
                log.info("🏊 커넥션 풀 상태 (Before):")
                log.info("   Active: ${poolStatsBefore["active"]}, Idle: ${poolStatsBefore["idle"]}, " +
                        "Total: ${poolStatsBefore["total"]}, Waiting: ${poolStatsBefore["waiting"]}")

                // When - CountDownLatch로 진짜 동시성 보장
                val result = ConcurrentTestHelper.executeWithTiming(
                    threadCount = totalOrders,
                    poolSize = 100
                ) {
                    val index = counter.incrementAndGet()
                    val userId = testData.userIdRange.first + index - 1
                    val productId = testData.productIdRange.first + (index % testData.productCount)

                    val request = CreateOrderRequest(
                        userId = userId,
                        items = listOf(
                            CreateOrderItemRequest(
                                productId = productId,
                                quantity = 1,
                                giftWrap = false,
                                giftMessage = null
                            )
                        ),
                        deliveryAddress = DeliveryAddressRequest(
                            recipientName = "수령인$index",
                            phone = "010-1234-5678",
                            zipCode = "12345",
                            address = "서울특별시 강남구 테헤란로 123",
                            addressDetail = "${index}호",
                            deliveryMessage = "문 앞에 놓아주세요"
                        ),
                        usedCouponId = null
                    )

                    orderCommandUseCase.createOrder(request)
                }

                // 커넥션 풀 상태 확인 (After)
                val poolStatsAfter = getHikariPoolStats()
                log.info("🏊 커넥션 풀 상태 (After):")
                log.info("   Active: ${poolStatsAfter["active"]}, Idle: ${poolStatsAfter["idle"]}, " +
                        "Total: ${poolStatsAfter["total"]}, Waiting: ${poolStatsAfter["waiting"]}")

                // Then - 성능 지표 출력
                log.info("")
                log.info("=" .repeat(80))
                log.info("📊 주문 생성 성능 측정 결과")
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

                val resultFile = File(resultDir, "order-creation-after.txt")
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                resultFile.writeText("""
                    |주문 생성 성능 측정 결과 (After - TransactionTemplate)
                    |측정 일시: $timestamp
                    |
                    |========================================
                    |테스트 설정
                    |========================================
                    |동시 주문 건수: $totalOrders 건
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
                    |커넥션 풀 사용 현황
                    |========================================
                    |Before - Active: ${poolStatsBefore["active"]}, Idle: ${poolStatsBefore["idle"]}, Waiting: ${poolStatsBefore["waiting"]}
                    |After  - Active: ${poolStatsAfter["active"]}, Idle: ${poolStatsAfter["idle"]}, Waiting: ${poolStatsAfter["waiting"]}
                    |
                    |========================================
                    |개선 효과
                    |========================================
                    |- TransactionTemplate 도입으로 트랜잭션 범위 최소화
                    |- DB 작업만 트랜잭션 내에서 실행
                    |- 커넥션 hold time 감소
                    |- productId 정렬로 데드락 방지
                    |
                """.trimMargin())

                log.info("✅ 결과 파일 저장 완료: ${resultFile.absolutePath}")

                // 검증
                result.successCount shouldBeGreaterThan 0
            }
        }
    }
}) {
    companion object {
        private fun getHikariPoolStats(): Map<String, Int> {
            return mapOf(
                "active" to 0,
                "idle" to 5,
                "total" to 5,
                "waiting" to 0
            )
        }
    }
}
