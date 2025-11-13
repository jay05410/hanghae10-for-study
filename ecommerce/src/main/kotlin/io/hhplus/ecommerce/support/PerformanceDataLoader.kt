package io.hhplus.ecommerce.support

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.user.domain.constant.LoginType
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * 성능 테스트용 대용량 데이터 로더
 *
 * 역할:
 * - 인덱스 성능 비교를 위한 충분한 양의 테스트 데이터 생성
 * - 실무 수준의 데이터 분포 시뮬레이션
 * - JDBC 배치 처리로 빠른 적재 성능 제공
 *
 * 사용법:
 * ```bash
 * # 데이터 로드 프로파일로 실행
 * ./gradlew bootRun --args='--spring.profiles.active=data-load'
 * ```
 *
 * 적재 데이터:
 * - Users: 10,000명
 * - Products: 10,000개
 * - Orders: 100,000건
 * - OrderItems: 300,000건
 * - PointHistory: 200,000건
 * - Inventory: 10,000건
 *
 * 총 예상 시간: 1~2분
 */
@Component
@Profile("data-load")
class PerformanceDataLoader(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // 데이터 볼륨 설정 (조정 가능)
    private val USER_COUNT = 10_000
    private val PRODUCT_COUNT = 10_000
    private val ORDER_COUNT = 100_000
    private val ORDER_ITEM_PER_ORDER = 3 // 평균 주문당 아이템 수
    private val POINT_HISTORY_COUNT = 200_000

    private val BATCH_SIZE = 1000 // 배치 크기

    override fun run(args: ApplicationArguments) {
        log.info("========================================")
        log.info("성능 테스트용 데이터 로드 시작")
        log.info("========================================")

        val totalTime = measureTimeMillis {
            try {
                // 기존 데이터 확인
                if (hasExistingData()) {
                    log.warn("⚠️  데이터가 이미 존재합니다. 스킵합니다.")
                    log.warn("   새로 적재하려면 DB를 초기화하세요: DROP DATABASE ecommerce; CREATE DATABASE ecommerce;")
                    return
                }

                loadUsers()
                loadProducts()
                loadInventory()
                loadOrders()
                loadOrderItems()
                loadPointHistory()
                loadUserPoints()

                log.info("========================================")
                log.info("✅ 데이터 로드 완료!")
                log.info("========================================")
                printSummary()
            } catch (e: Exception) {
                log.error("❌ 데이터 로드 실패", e)
                throw e
            }
        }

        log.info("총 소요 시간: ${totalTime / 1000.0}초")
    }

    private fun hasExistingData(): Boolean {
        val userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long::class.java) ?: 0
        return userCount > 0
    }

    @Transactional
    fun loadUsers() {
        log.info("👤 사용자 데이터 적재 시작 (목표: ${USER_COUNT}명)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO users (login_type, login_id, password, email, name, phone, provider_id, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = USER_COUNT / BATCH_SIZE
            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val userId = batch * BATCH_SIZE + i + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setString(1, LoginType.LOCAL.name)
                        ps.setString(2, "user$userId")
                        ps.setString(3, "password123")
                        ps.setString(4, "user$userId@test.com")
                        ps.setString(5, "테스트유저$userId")
                        ps.setString(6, String.format("010-%04d-%04d", userId / 10000, userId % 10000))
                        ps.setString(7, null)
                        ps.setBoolean(8, true)
                        ps.setString(9, now)
                        ps.setString(10, now)
                        ps.setLong(11, 0)
                        ps.setLong(12, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 10 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${USER_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 사용자 ${USER_COUNT}명 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadProducts() {
        log.info("📦 상품 데이터 적재 시작 (목표: ${PRODUCT_COUNT}개)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO items (category_id, name, description, caffeine_type, taste_profile, aroma_profile, color_profile, bag_per_weight, price_per_100g, ingredients, origin, status, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val caffeineTypes = listOf("NONE", "LOW", "MEDIUM", "HIGH")
            val tasteProfiles = listOf("MILD", "SWEET", "BITTER", "FRESH", "HERBAL")
            val aromaProfiles = listOf("FLORAL", "FRUITY", "WOODY", "FRESH", "SPICY")
            val colorProfiles = listOf("GOLDEN", "AMBER", "DARK", "LIGHT", "GREEN")
            val origins = listOf("한국", "중국", "일본", "인도", "스리랑카")

            val batches = PRODUCT_COUNT / BATCH_SIZE
            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val productId = batch * BATCH_SIZE + i + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, ((productId % 10) + 1).toLong()) // category_id: 1~10
                        ps.setString(2, "테스트 티 제품 #$productId")
                        ps.setString(3, "성능 테스트용 상품입니다. 상품번호: $productId")
                        ps.setString(4, caffeineTypes[productId % caffeineTypes.size])
                        ps.setString(5, tasteProfiles[productId % tasteProfiles.size])
                        ps.setString(6, aromaProfiles[productId % aromaProfiles.size])
                        ps.setString(7, colorProfiles[productId % colorProfiles.size])
                        ps.setInt(8, 3)
                        ps.setInt(9, Random.nextInt(5000, 30000)) // 5,000 ~ 30,000원
                        ps.setString(10, "차 잎 100%")
                        ps.setString(11, origins[productId % origins.size])
                        ps.setString(12, ProductStatus.ACTIVE.name)
                        ps.setBoolean(13, true)
                        ps.setString(14, now)
                        ps.setString(15, now)
                        ps.setLong(16, 0)
                        ps.setLong(17, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 10 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${PRODUCT_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 상품 ${PRODUCT_COUNT}개 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadInventory() {
        log.info("📊 재고 데이터 적재 시작 (목표: ${PRODUCT_COUNT}개)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO inventory (product_id, quantity, reserved_quantity, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = PRODUCT_COUNT / BATCH_SIZE
            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val productId = batch * BATCH_SIZE + i + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, productId.toLong())
                        ps.setLong(2, Random.nextLong(100, 10000)) // 재고: 100~10,000
                        ps.setLong(3, 0)
                        ps.setBoolean(4, true)
                        ps.setString(5, now)
                        ps.setString(6, now)
                        ps.setLong(7, 0)
                        ps.setLong(8, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })
            }
        }

        log.info("✅ 재고 ${PRODUCT_COUNT}개 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadOrders() {
        log.info("🛒 주문 데이터 적재 시작 (목표: ${ORDER_COUNT}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO orders (order_number, user_id, total_amount, discount_amount, final_amount, used_coupon_id, status, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statuses = OrderStatus.values()
            val batches = ORDER_COUNT / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val orderId = batch * BATCH_SIZE + i + 1
                        val userId = (orderId % USER_COUNT) + 1
                        val now = LocalDateTime.now().minusDays(Random.nextLong(0, 365)).format(dateFormatter)

                        val totalAmount = Random.nextLong(10000, 500000)
                        val discountAmount = if (Random.nextBoolean()) Random.nextLong(0, totalAmount / 10) else 0
                        val finalAmount = totalAmount - discountAmount

                        ps.setString(1, "ORD${orderId.toString().padStart(10, '0')}")
                        ps.setLong(2, userId.toLong())
                        ps.setLong(3, totalAmount)
                        ps.setLong(4, discountAmount)
                        ps.setLong(5, finalAmount)
                        ps.setObject(6, null)
                        ps.setString(7, statuses[orderId % statuses.size].name)
                        ps.setBoolean(8, true)
                        ps.setString(9, now)
                        ps.setString(10, now)
                        ps.setLong(11, userId.toLong())
                        ps.setLong(12, userId.toLong())
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 10 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${ORDER_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 주문 ${ORDER_COUNT}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadOrderItems() {
        val totalItems = ORDER_COUNT * ORDER_ITEM_PER_ORDER
        log.info("📋 주문 아이템 데이터 적재 시작 (목표: ${totalItems}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO order_items (order_id, product_id, quantity, price_per_unit, subtotal, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = totalItems / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val itemId = batch * BATCH_SIZE + i + 1
                        val orderId = (itemId / ORDER_ITEM_PER_ORDER) + 1
                        val productId = (itemId % PRODUCT_COUNT) + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        val quantity = Random.nextInt(1, 10)
                        val pricePerUnit = Random.nextLong(5000, 30000)
                        val subtotal = quantity * pricePerUnit

                        ps.setLong(1, orderId.toLong())
                        ps.setLong(2, productId.toLong())
                        ps.setInt(3, quantity)
                        ps.setLong(4, pricePerUnit)
                        ps.setLong(5, subtotal)
                        ps.setBoolean(6, true)
                        ps.setString(7, now)
                        ps.setString(8, now)
                        ps.setLong(9, 0)
                        ps.setLong(10, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 20 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${totalItems} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 주문 아이템 ${totalItems}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadPointHistory() {
        log.info("💰 포인트 히스토리 데이터 적재 시작 (목표: ${POINT_HISTORY_COUNT}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO point_histories (user_id, amount, transaction_type, balance_before, balance_after, order_id, description, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val transactionTypes = PointTransactionType.values()
            val batches = POINT_HISTORY_COUNT / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val historyId = batch * BATCH_SIZE + i + 1
                        val userId = (historyId % USER_COUNT) + 1
                        val now = LocalDateTime.now().minusDays(Random.nextLong(0, 365)).format(dateFormatter)

                        val transactionType = transactionTypes[historyId % transactionTypes.size]
                        val amount = Random.nextLong(100, 50000)
                        val balanceBefore = Random.nextLong(0, 1000000)
                        val balanceAfter = when (transactionType) {
                            PointTransactionType.EARN -> balanceBefore + amount
                            PointTransactionType.USE -> maxOf(0, balanceBefore - amount)
                            PointTransactionType.EXPIRE -> maxOf(0, balanceBefore - amount)
                            PointTransactionType.REFUND -> balanceBefore + amount
                        }

                        ps.setLong(1, userId.toLong())
                        ps.setLong(2, if (transactionType == PointTransactionType.EARN || transactionType == PointTransactionType.REFUND) amount else -amount)
                        ps.setString(3, transactionType.name)
                        ps.setLong(4, balanceBefore)
                        ps.setLong(5, balanceAfter)
                        ps.setObject(6, if (transactionType == PointTransactionType.USE) (historyId % ORDER_COUNT) + 1 else null)
                        ps.setString(7, "성능 테스트 데이터")
                        ps.setBoolean(8, true)
                        ps.setString(9, now)
                        ps.setString(10, now)
                        ps.setLong(11, userId.toLong())
                        ps.setLong(12, userId.toLong())
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 20 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${POINT_HISTORY_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 포인트 히스토리 ${POINT_HISTORY_COUNT}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadUserPoints() {
        log.info("💳 사용자 포인트 데이터 적재 시작 (목표: ${USER_COUNT}명)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO user_points (user_id, balance, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = USER_COUNT / BATCH_SIZE
            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val userId = batch * BATCH_SIZE + i + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, userId.toLong())
                        ps.setLong(2, Random.nextLong(0, 100000))
                        ps.setBoolean(3, true)
                        ps.setString(4, now)
                        ps.setString(5, now)
                        ps.setLong(6, 0)
                        ps.setLong(7, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })
            }
        }

        log.info("✅ 사용자 포인트 ${USER_COUNT}건 적재 완료 (${time}ms)")
    }

    private fun printSummary() {
        val counts = mapOf(
            "users" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long::class.java),
            "items" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM items", Long::class.java),
            "inventory" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory", Long::class.java),
            "orders" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long::class.java),
            "order_items" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_items", Long::class.java),
            "point_histories" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM point_histories", Long::class.java),
            "user_points" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_points", Long::class.java)
        )

        log.info("📊 데이터 적재 현황:")
        counts.forEach { (table, count) ->
            log.info("   - $table: ${String.format("%,d", count)}건")
        }

        val totalRecords = counts.values.sum()
        log.info("📊 총 레코드 수: ${String.format("%,d", totalRecords)}건")
    }
}
