package io.hhplus.ecommerce.support.performance

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 성능 테스트를 위한 대용량 테스트 데이터 생성 컴포넌트
 *
 * 역할:
 * - 성능 테스트에 필요한 사용자, 상품, 쿠폰, 포인트 데이터 생성
 * - Before/After 비교를 위한 일관된 테스트 환경 제공
 *
 * 사용법:
 * ```kotlin
 * val dataSetup = PerformanceTestDataSetup(...)
 * dataSetup.setupTestData()  // 테스트 데이터 생성
 * // ... 성능 테스트 실행 ...
 * dataSetup.cleanupTestData()  // 데이터 정리
 * ```
 */
@Component
class PerformanceTestDataSetup(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val couponRepository: CouponRepository,
    private val userPointRepository: UserPointRepository
) {

    private val log = KotlinLogging.logger {}

    companion object {
        const val TEST_USER_COUNT = 1000  // 대규모 테스트: 1,000명
        const val TEST_PRODUCT_COUNT = 50  // 대규모 테스트: 50개
        const val TEST_COUPON_COUNT = 10   // 대규모 테스트: 10개
        const val INITIAL_POINT_AMOUNT = 1_000_000L  // 각 사용자당 100만 포인트
    }

    /**
     * 성능 테스트용 대용량 데이터를 생성한다
     *
     * 생성되는 데이터:
     * - 100명의 사용자 (축소 규모)
     * - 10개의 상품 (축소 규모, categoryId=1 사용)
     * - 10개의 재고 (각 상품당 1000개)
     * - 5개의 쿠폰 (각 100개 한정, 축소 규모)
     * - 100개의 포인트 (각 사용자당 100만 포인트)
     *
     * @return 생성된 데이터의 ID 범위 정보
     */
    @Transactional
    fun setupTestData(): TestDataInfo {
        val startTime = System.currentTimeMillis()
        log.info("📝 성능 테스트 데이터 생성 시작...")

        // 1. 사용자 생성 (10,000명)
        val users = (1..TEST_USER_COUNT).map { index ->
            User.create(
                loginType = LoginType.LOCAL,
                loginId = "testuser${index}",
                password = "password${index}",
                name = "TestUser$index",
                email = "testuser${index}@test.com",
                phone = "010-${String.format("%04d", index / 10000)}-${String.format("%04d", index % 10000)}",
                providerId = null
            )
        }
        val savedUsers = users.map { userRepository.save(it) }
        log.info("✅ 사용자 ${savedUsers.size}명 생성 완료")

        // 2. 상품 생성 (10개)
        val products = (1..TEST_PRODUCT_COUNT).map { index ->
            Product.create(
                name = "TestProduct$index",
                description = "성능 테스트용 상품 #$index",
                price = (10_000 + (index * 1000)).toLong(),
                categoryId = 1L  // 기본 카테고리 ID (다른 테스트와 동일하게 사용)
            )
        }
        val savedProducts = products.map { productRepository.save(it) }
        log.info("✅ 상품 ${savedProducts.size}개 생성 완료")

        // 3. 재고 생성 (각 상품당 1000개)
        val inventories = savedProducts.map { product ->
            Inventory.create(
                productId = product.id,
                initialQuantity = 1000
            )
        }
        val savedInventories = inventories.map { inventoryRepository.save(it) }
        log.info("✅ 재고 ${savedInventories.size}개 생성 완료")

        // 4. 쿠폰 생성 (5개, 각 100개 한정)
        val now = LocalDateTime.now()
        val coupons = (1..TEST_COUPON_COUNT).map { index ->
            Coupon.create(
                name = "성능테스트 쿠폰 #$index",
                code = "PERF${String.format("%03d", index)}",
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10,
                minimumOrderAmount = 10000L,
                totalQuantity = 100,  // 각 쿠폰당 100개 한정
                validFrom = now,
                validTo = now.plusDays(30)
            )
        }
        val savedCoupons = coupons.map { couponRepository.save(it) }
        log.info("✅ 쿠폰 ${savedCoupons.size}개 생성 완료 (각 100개 한정)")

        // 5. 포인트 생성 (각 사용자당 100만 포인트)
        val userPoints = savedUsers.map { user ->
            val userPoint = UserPoint.create(userId = user.id)
            // 초기 포인트 적립 (주문 생성 테스트를 위해 필요)
            userPoint.earn(PointAmount.of(INITIAL_POINT_AMOUNT))
            userPoint
        }
        val savedUserPoints = userPoints.map { userPointRepository.save(it) }
        log.info("✅ 포인트 ${savedUserPoints.size}개 생성 완료 (각 ${INITIAL_POINT_AMOUNT}포인트)")

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        log.info("=" .repeat(80))
        log.info("✨ 성능 테스트 데이터 생성 완료!")
        log.info("   소요 시간: ${duration}ms (${duration / 1000}초)")
        log.info("   사용자: ${savedUsers.size}명")
        log.info("   상품: ${savedProducts.size}개")
        log.info("   재고: ${savedInventories.size}개")
        log.info("   쿠폰: ${savedCoupons.size}개")
        log.info("   포인트: ${savedUserPoints.size}개 (각 ${INITIAL_POINT_AMOUNT}포인트)")
        log.info("=" .repeat(80))

        return TestDataInfo(
            userIdRange = savedUsers.first().id..savedUsers.last().id,
            productIdRange = savedProducts.first().id..savedProducts.last().id,
            couponIdRange = savedCoupons.first().id..savedCoupons.last().id,
            userCount = savedUsers.size,
            productCount = savedProducts.size,
            couponCount = savedCoupons.size
        )
    }

    /**
     * 테스트 데이터를 정리한다
     * (KotestIntegrationTestBase의 afterTest가 자동으로 정리하므로 일반적으로 불필요)
     */
    fun cleanupTestData() {
        log.info("🧹 테스트 데이터 정리 (자동 정리로 생략)")
    }
}

/**
 * 생성된 테스트 데이터의 ID 범위 정보
 */
data class TestDataInfo(
    val userIdRange: LongRange,
    val productIdRange: LongRange,
    val couponIdRange: LongRange,
    val userCount: Int,
    val productCount: Int,
    val couponCount: Int
)
