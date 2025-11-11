package io.hhplus.ecommerce.coupon.infra

import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 사용자 쿠폰 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 사용자에게 발급된 쿠폰 데이터의 영속화 및 조회
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - UserCouponRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryUserCouponRepository : UserCouponRepository {
    private val userCoupons = ConcurrentHashMap<Long, UserCoupon>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        val userCoupon1 = UserCoupon.create(
            userId = 1L,
            couponId = 1L,
            createdBy = 1L
        ).let {
            UserCoupon(
                id = idGenerator.getAndIncrement(),
                userId = it.userId,
                couponId = it.couponId,
                issuedAt = now.minusDays(2),
                usedAt = null,
                usedOrderId = null,
                status = UserCouponStatus.ISSUED
            )
        }

        val userCoupon2 = UserCoupon.create(
            userId = 1L,
            couponId = 2L,
            createdBy = 1L
        ).let {
            UserCoupon(
                id = idGenerator.getAndIncrement(),
                userId = it.userId,
                couponId = it.couponId,
                issuedAt = now.minusDays(5),
                usedAt = now.minusDays(1),
                usedOrderId = 101L,
                status = UserCouponStatus.USED
            )
        }

        val userCoupon3 = UserCoupon.create(
            userId = 2L,
            couponId = 1L,
            createdBy = 1L
        ).let {
            UserCoupon(
                id = idGenerator.getAndIncrement(),
                userId = it.userId,
                couponId = it.couponId,
                issuedAt = now.minusDays(1),
                usedAt = null,
                usedOrderId = null,
                status = UserCouponStatus.ISSUED
            )
        }

        userCoupons[userCoupon1.id] = userCoupon1
        userCoupons[userCoupon2.id] = userCoupon2
        userCoupons[userCoupon3.id] = userCoupon3
    }

    /**
     * 사용자 쿠폰을 저장하거나 업데이트한다
     *
     * @param userCoupon 저장할 사용자 쿠폰 엔티티
     * @return 저장된 사용자 쿠폰 엔티티 (ID가 할당된 상태)
     */
    override fun save(userCoupon: UserCoupon): UserCoupon {
        simulateLatency()

        val savedUserCoupon = if (userCoupon.id == 0L) {
            UserCoupon(
                id = idGenerator.getAndIncrement(),
                userId = userCoupon.userId,
                couponId = userCoupon.couponId,
                issuedAt = userCoupon.issuedAt,
                usedAt = userCoupon.usedAt,
                usedOrderId = userCoupon.usedOrderId,
                status = userCoupon.status
            )
        } else {
            userCoupon
        }
        userCoupons[savedUserCoupon.id] = savedUserCoupon
        return savedUserCoupon
    }

    /**
     * 사용자 쿠폰 ID로 조회한다
     *
     * @param id 조회할 사용자 쿠폰의 ID
     * @return 사용자 쿠폰 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): UserCoupon? {
        simulateLatency()
        return userCoupons[id]
    }

    /**
     * 사용자 ID로 모든 사용자 쿠폰들을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 모든 쿠폰 목록
     */
    override fun findByUserId(userId: Long): List<UserCoupon> {
        simulateLatency()
        return userCoupons.values.filter { it.userId == userId }
    }

    /**
     * 쿠폰 ID로 모든 사용자 쿠폰들을 조회한다
     *
     * @param couponId 조회할 쿠폰의 ID
     * @return 쿠폰을 발급받은 모든 사용자 쿠폰 목록
     */
    override fun findByCouponId(couponId: Long): List<UserCoupon> {
        simulateLatency()
        return userCoupons.values.filter { it.couponId == couponId }
    }

    /**
     * 사용자 ID와 쿠폰 ID로 사용자 쿠폰을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param couponId 조회할 쿠폰의 ID
     * @return 조건에 맞는 사용자 쿠폰 (존재하지 않을 경우 null)
     */
    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon? {
        simulateLatency()
        return userCoupons.values.find { it.userId == userId && it.couponId == couponId }
    }

    /**
     * 사용자 ID와 쿠폰 코드로 사용자 쿠폰을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param couponCode 조회할 쿠폰의 코드
     * @return 조건에 맞는 사용자 쿠폰 (인메모리 구현에서는 null 반환)
     */
    override fun findByUserIdAndCouponCode(userId: Long, couponCode: String): UserCoupon? {
        simulateLatency()
        // 이 메서드는 Coupon repository에서 코드로 쿠폰을 찾아야 하므로, 현재 InMemory에서는 제한적
        // 실제 구현에서는 join이 필요
        return null
    }

    /**
     * 사용자 ID와 쿠폰 ID로 사용자 쿠폰 개수를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param couponId 조회할 쿠폰의 ID
     * @return 조건에 맞는 사용자 쿠폰 개수
     */
    override fun countByUserIdAndCouponId(userId: Long, couponId: Long): Long {
        simulateLatency()
        return userCoupons.values.count { it.userId == userId && it.couponId == couponId }.toLong()
    }

    /**
     * 사용자 ID와 상태로 사용자 쿠폰들을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param status 조회할 쿠폰 상태
     * @return 조건에 맞는 사용자 쿠폰 목록
     */
    override fun findByUserIdAndStatus(userId: Long, status: UserCouponStatus): List<UserCoupon> {
        simulateLatency()
        return userCoupons.values.filter { it.userId == userId && it.status == status }
    }

    /**
     * 실제 데이터베이스 지연시간을 시뮤레이션한다
     */
    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        userCoupons.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}