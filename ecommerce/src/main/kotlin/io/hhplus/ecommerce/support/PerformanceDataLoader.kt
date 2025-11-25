package io.hhplus.ecommerce.support

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.user.domain.constant.LoginType
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.DependsOn
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
 */
@Component
@Profile("data-load")
@DependsOn("entityManagerFactory")
class PerformanceDataLoader(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val log = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // 데이터 볼륨 설정 (조정 가능)
    private val USER_COUNT = 10_000
    private val PRODUCT_COUNT = 10_000
    private val ORDER_COUNT = 100_000
    private val ORDER_ITEM_PER_ORDER = 3 // 평균 주문당 아이템 수
    private val POINT_HISTORY_COUNT = 200_000
    private val COUPON_COUNT = 1_000
    private val USER_COUPON_COUNT = 5_000 // 사용자별 쿠폰 발급
    private val CART_COUNT = 3_000 // 장바구니 수 (일부 사용자만)
    private val CART_ITEM_PER_CART = 2 // 평균 장바구니당 아이템 수
    private val PAYMENT_COUNT = 80_000 // 주문의 80% 정도 결제
    private val PAYMENT_HISTORY_PER_PAYMENT = 2 // 평균 결제당 히스토리

    private val BATCH_SIZE = 1000 // 배치 크기

    override fun run(args: ApplicationArguments) {
        log.info("========================================")
        log.info("성능 테스트용 데이터 로드 시작")
        log.info("========================================")

        val totalTime = measureTimeMillis {
            try {
                loadUsersIfNeeded()
                loadProductsIfNeeded()
                loadInventoryIfNeeded()
                loadCouponsIfNeeded()
                loadUserCouponsIfNeeded()
                loadCartsIfNeeded()
                loadCartItemsIfNeeded()
                loadOrdersIfNeeded()
                loadOrderItemsIfNeeded()
                loadDeliveryIfNeeded()
                loadPaymentsIfNeeded()
                loadPaymentHistoryIfNeeded()
                loadPointHistoryIfNeeded()
                loadUserPointsIfNeeded()

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

    private fun clearExistingData() {
        log.info("🗑️  기존 데이터 클리어 중...")

        // 참조 무결성 때문에 순서대로 삭제
        val tables = listOf(
            "order_item_tea", "order_item", "point_history", "user_point",
            "stock_reservations", "delivery", "orders", "user_coupons",
            "coupon_issue_history", "cart_item_tea", "cart_items", "carts",
            "outbox_event", "product_statistics", "inventory", "items", "coupons", "users"
        )

        tables.forEach { table ->
            try {
                val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table", Long::class.java) ?: 0
                if (count > 0) {
                    jdbcTemplate.execute("DELETE FROM $table")
                    log.info("  $table: ${count}건 삭제")
                }
            } catch (e: Exception) {
                log.warn("  $table 테이블 삭제 중 오류 (무시): ${e.message}")
            }
        }

        // Auto increment 리셋
        tables.forEach { table ->
            try {
                jdbcTemplate.execute("ALTER TABLE $table AUTO_INCREMENT = 1")
            } catch (e: Exception) {
                // 일부 테이블은 AUTO_INCREMENT가 없을 수 있음
            }
        }

        log.info("✅ 기존 데이터 클리어 완료")
    }

    private fun loadUsersIfNeeded() {
        val count = getTableCount("users")
        if (count > 0) {
            log.info("👤 사용자 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadUsers()
    }

    private fun loadProductsIfNeeded() {
        val count = getTableCount("items")
        if (count > 0) {
            log.info("📦 상품 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadProducts()
    }

    private fun loadInventoryIfNeeded() {
        val count = getTableCount("inventory")
        if (count > 0) {
            log.info("📊 재고 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadInventory()
    }

    private fun loadOrdersIfNeeded() {
        val count = getTableCount("orders")
        if (count > 0) {
            log.info("🛒 주문 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadOrders()
    }

    private fun loadOrderItemsIfNeeded() {
        val count = getTableCount("order_item")
        if (count > 0) {
            log.info("📋 주문 아이템 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadOrderItems()
    }

    private fun loadPointHistoryIfNeeded() {
        val count = getTableCount("point_history")
        if (count > 0) {
            log.info("💰 포인트 히스토리 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadPointHistory()
    }

    private fun loadUserPointsIfNeeded() {
        val count = getTableCount("user_point")
        if (count > 0) {
            log.info("💳 사용자 포인트 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadUserPoints()
    }

    private fun loadCouponsIfNeeded() {
        val count = getTableCount("coupons")
        if (count > 0) {
            log.info("🎫 쿠폰 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadCoupons()
    }

    private fun loadUserCouponsIfNeeded() {
        val count = getTableCount("user_coupons")
        if (count > 0) {
            log.info("🎟️ 사용자 쿠폰 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadUserCoupons()
    }

    private fun loadCartsIfNeeded() {
        val count = getTableCount("carts")
        if (count > 0) {
            log.info("🛒 장바구니 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadCarts()
    }

    private fun loadCartItemsIfNeeded() {
        val count = getTableCount("cart_items")
        if (count > 0) {
            log.info("📝 장바구니 아이템 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadCartItems()
    }

    private fun loadDeliveryIfNeeded() {
        val count = getTableCount("delivery")
        if (count > 0) {
            log.info("🚚 배송 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadDelivery()
    }

    private fun loadPaymentsIfNeeded() {
        val count = getTableCount("payments")
        if (count > 0) {
            log.info("💳 결제 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadPayments()
    }

    private fun loadPaymentHistoryIfNeeded() {
        val count = getTableCount("payment_history")
        if (count > 0) {
            log.info("📋 결제 히스토리 데이터 이미 존재 (${count}건) - 건너뜀")
            return
        }
        loadPaymentHistory()
    }

    private fun getTableCount(tableName: String): Long {
        return try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $tableName", Long::class.java) ?: 0
        } catch (e: Exception) {
            0 // 테이블이 없으면 0 반환
        }
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
                INSERT INTO inventory (product_id, quantity, reserved_quantity, version, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                        ps.setInt(4, 0) // version
                        ps.setBoolean(5, true)
                        ps.setString(6, now)
                        ps.setString(7, now)
                        ps.setLong(8, 0)
                        ps.setLong(9, 0)
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
                INSERT INTO order_item (order_id, package_type_id, quantity, daily_serving, package_type_days, total_quantity, tea_price, container_price, gift_wrap_price, total_price, gift_wrap, package_type_name, gift_message, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = totalItems / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val itemId = batch * BATCH_SIZE + i + 1
                        val orderId = (itemId / ORDER_ITEM_PER_ORDER) + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        val quantity = Random.nextInt(1, 5)
                        val dailyServing = Random.nextInt(1, 4)
                        val packageTypeDays = Random.nextInt(7, 31)
                        val totalQuantity = quantity * dailyServing.toDouble()
                        val teaPrice = Random.nextInt(10000, 50000)
                        val containerPrice = Random.nextInt(5000, 15000)
                        val giftWrapPrice = if (Random.nextBoolean()) Random.nextInt(2000, 5000) else 0
                        val totalPrice = teaPrice + containerPrice + giftWrapPrice
                        val giftWrap = giftWrapPrice > 0

                        ps.setLong(1, orderId.toLong()) // order_id
                        ps.setLong(2, Random.nextLong(1, 11)) // package_type_id (1~10)
                        ps.setInt(3, quantity) // quantity
                        ps.setInt(4, dailyServing) // daily_serving
                        ps.setInt(5, packageTypeDays) // package_type_days
                        ps.setDouble(6, totalQuantity) // total_quantity
                        ps.setInt(7, teaPrice) // tea_price
                        ps.setInt(8, containerPrice) // container_price
                        ps.setInt(9, giftWrapPrice) // gift_wrap_price
                        ps.setInt(10, totalPrice) // total_price
                        ps.setBoolean(11, giftWrap) // gift_wrap
                        ps.setString(12, "패키지타입$packageTypeDays") // package_type_name
                        ps.setString(13, if (giftWrap) "성능테스트 선물메시지" else null) // gift_message
                        ps.setBoolean(14, true) // is_active
                        ps.setString(15, now) // created_at
                        ps.setString(16, now) // updated_at
                        ps.setLong(17, 0) // created_by
                        ps.setLong(18, 0) // updated_by
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
                INSERT INTO point_history (user_id, amount, transaction_type, balance_before, balance_after, order_id, description, is_active, created_at, updated_at, created_by, updated_by)
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
                INSERT INTO user_point (user_id, balance, version, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = USER_COUNT / BATCH_SIZE
            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val userId = batch * BATCH_SIZE + i + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, userId.toLong())
                        ps.setLong(2, Random.nextLong(0, 100000))
                        ps.setInt(3, 0) // version
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

        log.info("✅ 사용자 포인트 ${USER_COUNT}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadCoupons() {
        log.info("🎫 쿠폰 데이터 적재 시작 (목표: ${COUPON_COUNT}개)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO coupons (name, code, discount_type, discount_value, minimum_order_amount, total_quantity, issued_quantity, valid_from, valid_to, version, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val discountTypes = listOf("FIXED", "PERCENTAGE")
            val batches = COUPON_COUNT / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val couponId = batch * BATCH_SIZE + i + 1
                        val now = LocalDateTime.now().format(dateFormatter)
                        val validFrom = LocalDateTime.now().minusDays(30).format(dateFormatter)
                        val validTo = LocalDateTime.now().plusDays(30).format(dateFormatter)

                        val discountType = discountTypes[couponId % discountTypes.size]
                        val discountValue = if (discountType == "FIXED") {
                            Random.nextLong(1000, 10000) // 1,000 ~ 10,000원
                        } else {
                            Random.nextLong(5, 30) // 5% ~ 30%
                        }
                        val minimumOrderAmount = Random.nextLong(10000, 100000) // 10,000 ~ 100,000원
                        val totalQuantity = Random.nextInt(100, 10000)
                        val issuedQuantity = Random.nextInt(0, totalQuantity / 10)

                        ps.setString(1, "성능테스트쿠폰${couponId}")
                        ps.setString(2, "COUP${couponId.toString().padStart(6, '0')}")
                        ps.setString(3, discountType)
                        ps.setLong(4, discountValue)
                        ps.setLong(5, minimumOrderAmount)
                        ps.setInt(6, totalQuantity)
                        ps.setInt(7, issuedQuantity)
                        ps.setString(8, validFrom)
                        ps.setString(9, validTo)
                        ps.setInt(10, 0) // version
                        ps.setBoolean(11, true)
                        ps.setString(12, now)
                        ps.setString(13, now)
                        ps.setLong(14, 0)
                        ps.setLong(15, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 10 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${COUPON_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 쿠폰 ${COUPON_COUNT}개 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadUserCoupons() {
        log.info("🎟️ 사용자 쿠폰 데이터 적재 시작 (목표: ${USER_COUPON_COUNT}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO user_coupons (user_id, coupon_id, status, issued_at, used_at, used_order_id, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statuses = listOf("ISSUED", "USED", "EXPIRED")
            val batches = USER_COUPON_COUNT / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val userCouponId = batch * BATCH_SIZE + i + 1
                        val userId = (userCouponId % USER_COUNT) + 1
                        val couponId = (userCouponId % COUPON_COUNT) + 1
                        val status = statuses[userCouponId % statuses.size]
                        val issuedAt = LocalDateTime.now().minusDays(Random.nextLong(0, 60)).format(dateFormatter)
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, userId.toLong())
                        ps.setLong(2, couponId.toLong())
                        ps.setString(3, status)
                        ps.setString(4, issuedAt)
                        ps.setObject(5, if (status == "USED") LocalDateTime.now().minusDays(Random.nextLong(0, 30)).format(dateFormatter) else null)
                        ps.setObject(6, if (status == "USED") (userCouponId % ORDER_COUNT) + 1 else null)
                        ps.setBoolean(7, true)
                        ps.setString(8, now)
                        ps.setString(9, now)
                        ps.setLong(10, 0)
                        ps.setLong(11, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })
            }
        }

        log.info("✅ 사용자 쿠폰 ${USER_COUPON_COUNT}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadCarts() {
        log.info("🛒 장바구니 데이터 적재 시작 (목표: ${CART_COUNT}개)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO carts (user_id, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = CART_COUNT / BATCH_SIZE
            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val cartId = batch * BATCH_SIZE + i + 1
                        val userId = cartId // 사용자별로 1개씩
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, userId.toLong())
                        ps.setBoolean(2, true)
                        ps.setString(3, now)
                        ps.setString(4, now)
                        ps.setLong(5, userId.toLong())
                        ps.setLong(6, userId.toLong())
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })
            }
        }

        log.info("✅ 장바구니 ${CART_COUNT}개 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadCartItems() {
        val totalItems = CART_COUNT * CART_ITEM_PER_CART
        log.info("📝 장바구니 아이템 데이터 적재 시작 (목표: ${totalItems}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO cart_items (cart_id, package_type_id, daily_serving, gift_wrap, package_type_days, package_type_name, total_quantity, gift_message, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val batches = totalItems / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val itemId = batch * BATCH_SIZE + i + 1
                        val cartId = (itemId / CART_ITEM_PER_CART) + 1
                        val now = LocalDateTime.now().format(dateFormatter)

                        val dailyServing = Random.nextInt(1, 4)
                        val packageTypeDays = Random.nextInt(7, 31)
                        val totalQuantity = dailyServing * packageTypeDays.toDouble()
                        val giftWrap = Random.nextBoolean()

                        ps.setLong(1, cartId.toLong())
                        ps.setLong(2, Random.nextLong(1, 11))
                        ps.setInt(3, dailyServing)
                        ps.setBoolean(4, giftWrap)
                        ps.setInt(5, packageTypeDays)
                        ps.setString(6, "패키지타입${packageTypeDays}")
                        ps.setDouble(7, totalQuantity)
                        ps.setString(8, if (giftWrap) "장바구니 선물 메시지" else null)
                        ps.setBoolean(9, true)
                        ps.setString(10, now)
                        ps.setString(11, now)
                        ps.setLong(12, 0)
                        ps.setLong(13, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })
            }
        }

        log.info("✅ 장바구니 아이템 ${totalItems}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadDelivery() {
        log.info("🚚 배송 데이터 적재 시작 (목표: ${ORDER_COUNT}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO delivery (order_id, status, delivery_address, carrier, tracking_number, shipped_at, delivered_at, delivery_memo, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statuses = listOf("PENDING", "PREPARING", "SHIPPED", "DELIVERED", "FAILED")
            val carriers = listOf("CJ대한통운", "한진택배", "롯데택배", "우체국택배", "로젠택배")
            val batches = ORDER_COUNT / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val orderId = batch * BATCH_SIZE + i + 1
                        val status = statuses[orderId % statuses.size]
                        val carrier = carriers[orderId % carriers.size]
                        val now = LocalDateTime.now().format(dateFormatter)
                        val address = """{"zipCode":"${Random.nextInt(10000,99999)}","address":"서울시 강남구 테헤란로 ${Random.nextInt(1,500)}","detailAddress":"${Random.nextInt(1,20)}층"}"""

                        ps.setLong(1, orderId.toLong())
                        ps.setString(2, status)
                        ps.setString(3, address)
                        ps.setString(4, carrier)
                        ps.setString(5, if (status in listOf("SHIPPED", "DELIVERED")) "${Random.nextLong(100000000000, 999999999999)}" else null)
                        ps.setObject(6, if (status in listOf("SHIPPED", "DELIVERED")) LocalDateTime.now().minusDays(Random.nextLong(0, 7)).format(dateFormatter) else null)
                        ps.setObject(7, if (status == "DELIVERED") LocalDateTime.now().minusDays(Random.nextLong(0, 3)).format(dateFormatter) else null)
                        ps.setString(8, "문앞에 놓아주세요")
                        ps.setBoolean(9, true)
                        ps.setString(10, now)
                        ps.setString(11, now)
                        ps.setLong(12, 0)
                        ps.setLong(13, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 10 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${ORDER_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 배송 ${ORDER_COUNT}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadPayments() {
        log.info("💳 결제 데이터 적재 시작 (목표: ${PAYMENT_COUNT}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO payments (user_id, order_id, payment_number, amount, payment_method, status, external_transaction_id, failure_reason, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val paymentMethods = listOf("CARD", "BANK_TRANSFER", "BALANCE")
            val statuses = listOf("PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED")
            val batches = PAYMENT_COUNT / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val paymentId = batch * BATCH_SIZE + i + 1
                        val orderId = paymentId // 1:1 매핑
                        val userId = (orderId % USER_COUNT) + 1
                        val paymentMethod = paymentMethods[paymentId % paymentMethods.size]
                        val status = statuses[paymentId % statuses.size]
                        val amount = Random.nextLong(10000, 500000)
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, userId.toLong())
                        ps.setLong(2, orderId.toLong())
                        ps.setString(3, "PAY${paymentId.toString().padStart(10, '0')}")
                        ps.setLong(4, amount)
                        ps.setString(5, paymentMethod)
                        ps.setString(6, status)
                        ps.setString(7, if (status in listOf("COMPLETED", "FAILED")) "ext_${Random.nextLong(100000, 999999)}" else null)
                        ps.setString(8, if (status == "FAILED") "카드 한도 초과" else null)
                        ps.setString(9, now)
                        ps.setString(10, now)
                        ps.setLong(11, userId.toLong())
                        ps.setLong(12, userId.toLong())
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })

                if ((batch + 1) % 10 == 0) {
                    log.info("  진행: ${(batch + 1) * BATCH_SIZE}/${PAYMENT_COUNT} (${(batch + 1) * 100 / batches}%)")
                }
            }
        }

        log.info("✅ 결제 ${PAYMENT_COUNT}건 적재 완료 (${time}ms)")
    }

    @Transactional
    fun loadPaymentHistory() {
        val totalHistory = PAYMENT_COUNT * PAYMENT_HISTORY_PER_PAYMENT
        log.info("📋 결제 히스토리 데이터 적재 시작 (목표: ${totalHistory}건)")

        val time = measureTimeMillis {
            val sql = """
                INSERT INTO payment_history (payment_id, amount, status_before, status_after, reason, pg_response, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statusTransitions = listOf(
                "PENDING" to "PROCESSING",
                "PROCESSING" to "COMPLETED",
                "PROCESSING" to "FAILED",
                "COMPLETED" to "CANCELLED"
            )
            val batches = totalHistory / BATCH_SIZE

            for (batch in 0 until batches) {
                jdbcTemplate.batchUpdate(sql, object : org.springframework.jdbc.core.BatchPreparedStatementSetter {
                    override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                        val historyId = batch * BATCH_SIZE + i + 1
                        val paymentId = (historyId / PAYMENT_HISTORY_PER_PAYMENT) + 1
                        val transition = statusTransitions[historyId % statusTransitions.size]
                        val amount = Random.nextLong(10000, 500000)
                        val now = LocalDateTime.now().format(dateFormatter)

                        ps.setLong(1, paymentId.toLong())
                        ps.setLong(2, amount)
                        ps.setString(3, transition.first)
                        ps.setString(4, transition.second)
                        ps.setString(5, "자동 상태 변경")
                        ps.setString(6, """{"code":"${Random.nextInt(1000,9999)}","message":"처리완료"}""")
                        ps.setString(7, now)
                        ps.setString(8, now)
                        ps.setLong(9, 0)
                        ps.setLong(10, 0)
                    }

                    override fun getBatchSize(): Int = BATCH_SIZE
                })
            }
        }

        log.info("✅ 결제 히스토리 ${totalHistory}건 적재 완료 (${time}ms)")
    }

    private fun printSummary() {
        val counts = mapOf(
            "users" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long::class.java),
            "items" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM items", Long::class.java),
            "inventory" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory", Long::class.java),
            "coupons" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coupons", Long::class.java),
            "user_coupons" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_coupons", Long::class.java),
            "carts" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carts", Long::class.java),
            "cart_items" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items", Long::class.java),
            "orders" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long::class.java),
            "order_item" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_item", Long::class.java),
            "delivery" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery", Long::class.java),
            "payments" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Long::class.java),
            "payment_history" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_history", Long::class.java),
            "point_history" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM point_history", Long::class.java),
            "user_point" to jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_point", Long::class.java)
        )

        log.info("📊 데이터 적재 현황:")
        counts.forEach { (table, count) ->
            log.info("   - $table: ${String.format("%,d", count)}건")
        }

        val totalRecords = counts.values.filterNotNull().sum()
        log.info("📊 총 레코드 수: ${String.format("%,d", totalRecords)}건")
    }
}
