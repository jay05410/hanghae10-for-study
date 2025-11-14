package io.hhplus.ecommerce.support.performance

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import kotlin.system.measureTimeMillis

/**
 * 인덱스 성능 비교 테스트
 *
 * 목적:
 * - 인덱스 추가 전후 쿼리 성능 비교
 * - 쿼리 보고서 작성을 위한 실제 성능 데이터 수집
 *
 * 전제 조건:
 * - PerformanceDataLoader로 대용량 데이터 적재 완료 (기존 Docker 컨테이너)
 *
 * 사용법:
 * 1. 인덱스 없이 테스트 실행:
 *    ./gradlew test --tests IndexPerformanceComparisonTest
 *
 * 2. 인덱스 추가:
 *    MySQL에 접속하여 인덱스 생성 SQL 실행
 *
 * 3. 인덱스 있는 상태로 다시 테스트 실행:
 *    ./gradlew test --tests IndexPerformanceComparisonTest
 *
 * 4. 성능 로그 비교 분석
 */
@SpringBootTest(
    properties = [
        "spring.jpa.show-sql=false",  // 성능 측정 시 로그 최소화
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
    ]
)
@ActiveProfiles("default")  // 기존 Docker 컨테이너 사용
class IndexPerformanceComparisonTest(
    private val jdbcTemplate: JdbcTemplate
) : DescribeSpec() {

    override fun extensions() = listOf(SpringExtension)

    private val log = LoggerFactory.getLogger(IndexPerformanceComparisonTest::class.java)

    init {

    /**
     * 시나리오 1: 사용자별 주문 조회 (user_id로 조회)
     *
     * 비즈니스 상황: 사용자가 "내 주문 목록" 페이지를 조회
     * 예상 개선: user_id 인덱스 추가 시 50~90% 성능 향상
     */
    describe("시나리오 1: 사용자별 주문 조회 성능") {
        it("user_id로 주문 조회 - 단일 사용자") {
            val userId = 5000L

            // 캐시 워밍업
            jdbcTemplate.query("SELECT * FROM orders WHERE user_id = ? LIMIT 1", { rs, _ -> rs.getLong("id") }, userId)

            // 실제 측정
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC",
                    { rs, _ -> rs.getLong("id") },
                    userId
                )
            }

            log.info("✅ 사용자별 주문 조회 (user_id=$userId): ${time}ms")
        }

        it("user_id로 주문 조회 - 100명 반복 (평균 성능)") {
            val times = mutableListOf<Long>()

            for (userId in 1L..100L) {
                val time = measureTimeMillis {
                    jdbcTemplate.query(
                        "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 10",
                        { rs, _ -> rs.getLong("id") },
                        userId
                    )
                }
                times.add(time)
            }

            val avgTime = times.average()
            val maxTime = times.maxOrNull() ?: 0
            val minTime = times.minOrNull() ?: 0

            log.info("📊 사용자별 주문 조회 통계 (100명 반복):")
            log.info("   평균: ${String.format("%.2f", avgTime)}ms")
            log.info("   최대: ${maxTime}ms")
            log.info("   최소: ${minTime}ms")
        }
    }

    /**
     * 시나리오 2: 주문 상태별 조회
     *
     * 비즈니스 상황: 관리자가 "처리 대기 중인 주문" 목록 조회
     * 예상 개선: status 인덱스 추가 시 60~80% 성능 향상
     */
    describe("시나리오 2: 주문 상태별 조회 성능") {
        it("status로 주문 조회 - PENDING 상태") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM orders WHERE status = ? ORDER BY created_at DESC LIMIT 100",
                    { rs, _ -> rs.getLong("id") },
                    "PENDING"
                )
            }

            log.info("✅ 주문 상태별 조회 (status=PENDING): ${time}ms")
        }

        it("복합 조건 조회 - user_id + status") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM orders WHERE user_id = ? AND status = ? ORDER BY created_at DESC",
                    { rs, _ -> rs.getLong("id") },
                    1000L,
                    "COMPLETED"
                )
            }

            log.info("✅ 복합 조건 조회 (user_id + status): ${time}ms")
        }
    }

    /**
     * 시나리오 3: 포인트 히스토리 조회
     *
     * 비즈니스 상황: 사용자가 "포인트 사용 내역" 페이지 조회
     * 예상 개선: user_id 인덱스 추가 시 70~90% 성능 향상
     */
    describe("시나리오 3: 포인트 히스토리 조회 성능") {
        it("user_id로 포인트 히스토리 조회") {
            val userId = 5000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM point_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 20",
                    { rs, _ -> rs.getLong("id") },
                    userId
                )
            }

            log.info("✅ 포인트 히스토리 조회 (user_id=$userId): ${time}ms")
        }

        it("특정 기간 포인트 히스토리 조회") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    """
                    SELECT * FROM point_history
                    WHERE user_id = ?
                      AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                    ORDER BY created_at DESC
                    """.trimIndent(),
                    { rs, _ -> rs.getLong("id") },
                    5000L
                )
            }

            log.info("✅ 특정 기간 포인트 히스토리 조회: ${time}ms")
        }
    }

    /**
     * 시나리오 4: 상품 검색 및 필터링
     *
     * 비즈니스 상황: 사용자가 상품 카테고리별, 상태별 필터링
     * 예상 개선: category_id, status 인덱스 추가 시 50~70% 성능 향상
     */
    describe("시나리오 4: 상품 검색 성능") {
        it("카테고리별 활성 상품 조회") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM items WHERE category_id = ? AND is_active = true LIMIT 50",
                    { rs, _ -> rs.getLong("id") },
                    5L
                )
            }

            log.info("✅ 카테고리별 상품 조회: ${time}ms")
        }

        it("상품명 LIKE 검색") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM items WHERE name LIKE ? LIMIT 50",
                    { rs, _ -> rs.getLong("id") },
                    "%티 제품%"
                )
            }

            log.info("✅ 상품명 LIKE 검색: ${time}ms")
        }
    }

    /**
     * 시나리오 5: JOIN 쿼리 성능
     *
     * 비즈니스 상황: 주문 상세 조회 (주문 + 주문 아이템 + 상품 정보)
     * 예상 개선: FK 인덱스 추가 시 80~95% 성능 향상
     */
    describe("시나리오 5: JOIN 쿼리 성능") {
        it("주문 상세 정보 조회 (Order + OrderItem + Product)") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    """
                    SELECT
                        o.id, o.order_number, o.user_id, o.total_amount,
                        oi.package_type_id, oi.quantity, oi.total_price,
                        i.name as product_name
                    FROM orders o
                    INNER JOIN order_item oi ON o.id = oi.order_id
                    INNER JOIN items i ON oi.package_type_id = i.id
                    WHERE o.user_id = ?
                    ORDER BY o.created_at DESC
                    LIMIT 100
                    """.trimIndent(),
                    { rs, _ -> rs.getLong("id") },
                    5000L
                )
            }

            log.info("✅ 주문 상세 정보 조회 (3개 테이블 JOIN): ${time}ms")
        }

        it("사용자 주문 통계 (GROUP BY, COUNT)") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    """
                    SELECT
                        user_id,
                        COUNT(*) as order_count,
                        SUM(final_amount) as total_spent
                    FROM orders
                    WHERE user_id BETWEEN ? AND ?
                    GROUP BY user_id
                    """.trimIndent(),
                    { rs, _ -> rs.getLong("user_id") },
                    1L,
                    1000L
                )
            }

            log.info("✅ 사용자 주문 통계 (GROUP BY): ${time}ms")
        }
    }

    /**
     * 시나리오 6: Payment (결제) 관련 쿼리 성능
     *
     * 비즈니스 상황: 주문별/사용자별 결제 내역 조회
     * 예상 개선: 이미 인덱스 있음 - 성능 검증 목적
     */
    describe("시나리오 6: Payment 관련 쿼리 성능") {
        it("주문별 결제 내역 조회") {
            val orderId = 50000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC",
                    { rs, _ -> rs.getLong("id") },
                    orderId
                )
            }

            log.info("✅ 주문별 결제 내역 조회 (order_id=$orderId): ${time}ms")
        }

        it("사용자별 결제 내역 조회") {
            val userId = 5000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM payments WHERE user_id = ? ORDER BY created_at DESC LIMIT 20",
                    { rs, _ -> rs.getLong("id") },
                    userId
                )
            }

            log.info("✅ 사용자별 결제 내역 조회 (user_id=$userId): ${time}ms")
        }

        it("결제 상태별 조회") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM payments WHERE status = ? ORDER BY created_at DESC LIMIT 100",
                    { rs, _ -> rs.getLong("id") },
                    "COMPLETED"
                )
            }

            log.info("✅ 결제 상태별 조회 (status=COMPLETED): ${time}ms")
        }
    }

    /**
     * 시나리오 7: Cart (장바구니) 관련 쿼리 성능
     *
     * 비즈니스 상황: 사용자별 장바구니 조회
     * 예상 개선: 이미 unique 인덱스 있음
     */
    describe("시나리오 7: Cart 관련 쿼리 성능") {
        it("사용자별 장바구니 조회") {
            val userId = 5000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM carts WHERE user_id = ? AND is_active = true",
                    { rs, _ -> rs.getLong("id") },
                    userId
                )
            }

            log.info("✅ 사용자별 장바구니 조회 (user_id=$userId): ${time}ms")
        }

        it("장바구니 아이템 조회 (Cart + Item JOIN)") {
            val userId = 5000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    """
                    SELECT
                        c.id as cart_id,
                        ci.id as cart_item_id,
                        ci.package_type_id,
                        ci.total_quantity,
                        ci.package_type_name
                    FROM carts c
                    INNER JOIN cart_items ci ON c.id = ci.cart_id
                    WHERE c.user_id = ? AND c.is_active = true
                    """.trimIndent(),
                    { rs, _ -> rs.getLong("cart_id") },
                    userId
                )
            }

            log.info("✅ 장바구니 아이템 조회 (Cart + Item JOIN): ${time}ms")
        }
    }

    /**
     * 시나리오 8: Delivery (배송) 관련 쿼리 성능
     *
     * 비즈니스 상황: 주문별 배송 상태 조회
     * 예상 개선: 이미 인덱스 있음
     */
    describe("시나리오 8: Delivery 관련 쿼리 성능") {
        it("주문별 배송 상태 조회") {
            val orderId = 50000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM delivery WHERE order_id = ?",
                    { rs, _ -> rs.getLong("id") },
                    orderId
                )
            }

            log.info("✅ 주문별 배송 상태 조회 (order_id=$orderId): ${time}ms")
        }

        it("배송 상태별 조회") {
            val time = measureTimeMillis {
                jdbcTemplate.query(
                    "SELECT * FROM delivery WHERE status = ? ORDER BY created_at DESC LIMIT 100",
                    { rs, _ -> rs.getLong("id") },
                    "DELIVERED"
                )
            }

            log.info("✅ 배송 상태별 조회 (status=DELIVERED): ${time}ms")
        }
    }

    /**
     * 시나리오 9: 복합 JOIN 쿼리 성능 (4-way JOIN)
     *
     * 비즈니스 상황: 사용자 주문 전체 정보 조회 (Order + Payment + Delivery + OrderItem)
     * 예상 개선: order_item FK 인덱스 없어서 느림 예상
     */
    describe("시나리오 9: 복합 JOIN 쿼리 성능 (4-way)") {
        it("사용자 주문 전체 정보 조회 (Order + Payment + Delivery + OrderItem)") {
            val userId = 5000L

            val time = measureTimeMillis {
                jdbcTemplate.query(
                    """
                    SELECT
                        o.id as order_id,
                        o.order_number,
                        o.total_amount,
                        p.payment_number,
                        p.status as payment_status,
                        d.tracking_number,
                        d.status as delivery_status,
                        oi.quantity,
                        oi.total_price
                    FROM orders o
                    LEFT JOIN payments p ON o.id = p.order_id
                    LEFT JOIN delivery d ON o.id = d.order_id
                    LEFT JOIN order_item oi ON o.id = oi.order_id
                    WHERE o.user_id = ?
                    ORDER BY o.created_at DESC
                    LIMIT 50
                    """.trimIndent(),
                    { rs, _ -> rs.getLong("order_id") },
                    userId
                )
            }

            log.info("✅ 4-way JOIN 쿼리 (Order + Payment + Delivery + OrderItem): ${time}ms")
        }
    }

    /**
     * 시나리오 10: 인덱스 효과 검증 (EXPLAIN ANALYZE)
     */
    describe("시나리오 10: 쿼리 실행 계획 분석") {
        it("EXPLAIN으로 쿼리 실행 계획 확인") {
            val query = "SELECT * FROM orders WHERE user_id = 5000 ORDER BY created_at DESC"

            val explainResult = jdbcTemplate.queryForList("EXPLAIN $query")

            log.info("📋 쿼리 실행 계획:")
            log.info("   쿼리: $query")
            explainResult.forEach { row ->
                log.info("   type: ${row["type"]}, key: ${row["key"]}, rows: ${row["rows"]}, Extra: ${row["Extra"]}")
            }
        }

        it("현재 테이블별 인덱스 목록 확인") {
            val tables = listOf("users", "items", "orders", "order_item", "point_history", "inventory")

            tables.forEach { table ->
                val indexes = jdbcTemplate.queryForList("SHOW INDEX FROM $table")
                log.info("📊 [$table] 테이블 인덱스:")
                indexes.forEach { index ->
                    log.info("   - ${index["Key_name"]} (${index["Column_name"]}), Non_unique: ${index["Non_unique"]}")
                }
            }
        }
    }

    /**
     * 성능 비교 요약
     */
    describe("성능 측정 요약") {
        it("전체 시나리오 종합 측정") {
            log.info("")
            log.info("=" .repeat(80))
            log.info("📊 성능 측정 완료")
            log.info("=" .repeat(80))
        }
    }
    }
}
