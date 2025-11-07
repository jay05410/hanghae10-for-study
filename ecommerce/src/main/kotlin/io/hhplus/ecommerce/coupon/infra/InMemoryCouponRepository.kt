package io.hhplus.ecommerce.coupon.infra

import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 쿠폰 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 쿠폰 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - CouponRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryCouponRepository : CouponRepository {
    private val coupons = ConcurrentHashMap<Long, Coupon>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val now = java.time.LocalDateTime.now()

        val welcomeCoupon = Coupon.create(
            name = "신규 가입 쿠폰",
            code = "WELCOME2024",
            discountType = io.hhplus.ecommerce.coupon.domain.constant.DiscountType.FIXED,
            discountValue = 5000L,
            minimumOrderAmount = 20000L,
            totalQuantity = 1000,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
            createdBy = 1L
        ).let {
            Coupon(
                id = idGenerator.getAndIncrement(),
                name = it.name,
                code = it.code,
                discountType = it.discountType,
                discountValue = it.discountValue,
                minimumOrderAmount = it.minimumOrderAmount,
                totalQuantity = it.totalQuantity,
                issuedQuantity = 45,
                version = it.version,
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }

        val percentCoupon = Coupon.create(
            name = "봄맞이 할인 쿠폰",
            code = "SPRING20",
            discountType = io.hhplus.ecommerce.coupon.domain.constant.DiscountType.PERCENTAGE,
            discountValue = 20L,
            minimumOrderAmount = 50000L,
            totalQuantity = 500,
            validFrom = now.minusDays(10),
            validTo = now.plusDays(20),
            createdBy = 1L
        ).let {
            Coupon(
                id = idGenerator.getAndIncrement(),
                name = it.name,
                code = it.code,
                discountType = it.discountType,
                discountValue = it.discountValue,
                minimumOrderAmount = it.minimumOrderAmount,
                totalQuantity = it.totalQuantity,
                issuedQuantity = 123,
                version = it.version,
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }

        val vipCoupon = Coupon.create(
            name = "VIP 전용 쿠폰",
            code = "VIP10000",
            discountType = io.hhplus.ecommerce.coupon.domain.constant.DiscountType.FIXED,
            discountValue = 10000L,
            minimumOrderAmount = 100000L,
            totalQuantity = 100,
            validFrom = now.minusDays(5),
            validTo = now.plusDays(60),
            createdBy = 1L
        ).let {
            Coupon(
                id = idGenerator.getAndIncrement(),
                name = it.name,
                code = it.code,
                discountType = it.discountType,
                discountValue = it.discountValue,
                minimumOrderAmount = it.minimumOrderAmount,
                totalQuantity = it.totalQuantity,
                issuedQuantity = 23,
                version = it.version,
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }

        coupons[welcomeCoupon.id] = welcomeCoupon
        coupons[percentCoupon.id] = percentCoupon
        coupons[vipCoupon.id] = vipCoupon
    }

    /**
     * 쿠폰을 저장하거나 업데이트한다
     *
     * @param coupon 저장할 쿠폰 엔티티
     * @return 저장된 쿠폰 엔티티 (ID가 할당된 상태)
     */
    override fun save(coupon: Coupon): Coupon {
        simulateLatency()

        val savedCoupon = if (coupon.id == 0L) {
            Coupon(
                id = idGenerator.getAndIncrement(),
                name = coupon.name,
                code = coupon.code,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                minimumOrderAmount = coupon.minimumOrderAmount,
                totalQuantity = coupon.totalQuantity,
                issuedQuantity = coupon.issuedQuantity,
                version = coupon.version,
                validFrom = coupon.validFrom,
                validTo = coupon.validTo
            )
        } else {
            coupon
        }
        coupons[savedCoupon.id] = savedCoupon
        return savedCoupon
    }

    /**
     * 쿠폰 ID로 쿠폰을 조회한다
     *
     * @param id 조회할 쿠폰의 ID
     * @return 쿠폰 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): Coupon? {
        simulateLatency()
        return coupons[id]
    }

    /**
     * 쿠폰 ID로 쿠폰을 업데이트 락과 함께 조회한다
     *
     * @param id 조회할 쿠폰의 ID
     * @return 쿠폰 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByIdWithLock(id: Long): Coupon? {
        simulateLatency()
        return findById(id)
    }

    /**
     * 쿠폰 이름으로 쿠폰을 조회한다
     *
     * @param name 조회할 쿠폰의 이름
     * @return 쿠폰 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByName(name: String): Coupon? {
        simulateLatency()
        return coupons.values.find { it.name == name }
    }

    /**
     * 쿠폰 코드로 쿠폰을 조회한다
     *
     * @param code 조회할 쿠폰의 코드
     * @return 쿠폰 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByCode(code: String): Coupon? {
        simulateLatency()
        return coupons.values.find { it.code == code }
    }

    /**
     * 발급 가능한 쿠폰들을 조회한다
     *
     * @return 발급 가능한 쿠폰 목록
     */
    override fun findAvailableCoupons(): List<Coupon> {
        simulateLatency()
        return coupons.values.filter { it.isAvailableForIssue() }
    }

    /**
     * 활성 상태인 쿠폰들을 조회한다
     *
     * @return 활성 상태의 쿠폰 목록
     */
    override fun findByIsActiveTrue(): List<Coupon> {
        simulateLatency()
        return coupons.values.filter { it.isActive }
    }

    /**
     * 유효 기간 범위로 쿠폰들을 조회한다
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 유효 기간에 포함되는 쿠폰 목록
     */
    override fun findByValidDateRange(startDate: java.time.LocalDateTime, endDate: java.time.LocalDateTime): List<Coupon> {
        simulateLatency()
        return coupons.values.filter {
            it.validFrom.isAfter(startDate) && it.validTo.isBefore(endDate)
        }
    }

    /**
     * 만료된 쿠폰들을 조회한다
     *
     * @param currentDate 기준 날짜
     * @return 만료된 쿠폰 목록
     */
    override fun findExpiredCoupons(currentDate: java.time.LocalDateTime): List<Coupon> {
        simulateLatency()
        return coupons.values.filter { it.validTo.isBefore(currentDate) }
    }

    /**
     * 쿠폰을 삭제한다
     *
     * @param coupon 삭제할 쿠폰 엔티티
     */
    override fun delete(coupon: Coupon) {
        simulateLatency()
        coupons.remove(coupon.id)
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
        coupons.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}