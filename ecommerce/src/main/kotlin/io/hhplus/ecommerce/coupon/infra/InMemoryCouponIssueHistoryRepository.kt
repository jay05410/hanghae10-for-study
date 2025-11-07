package io.hhplus.ecommerce.coupon.infra

import io.hhplus.ecommerce.coupon.domain.entity.CouponIssueHistory
import io.hhplus.ecommerce.coupon.domain.repository.CouponIssueHistoryRepository
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 쿠폰 발급 이력 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 쿠폰 발급, 사용, 만료 이력 데이터의 영속화 및 조회
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - CouponIssueHistoryRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryCouponIssueHistoryRepository : CouponIssueHistoryRepository {
    private val storage = ConcurrentHashMap<Long, CouponIssueHistory>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        val issuedHistory1 = CouponIssueHistory.createIssueHistory(
            couponId = 1L,
            userId = 1L,
            issuedAt = now.minusDays(5),
            description = "신규 가입 쿠폰 발급",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val usedHistory1 = CouponIssueHistory.createUsageHistory(
            couponId = 1L,
            userId = 1L,
            issuedAt = now.minusDays(3),
            usedAt = now.minusDays(1),
            description = "주문 시 사용",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val issuedHistory2 = CouponIssueHistory.createIssueHistory(
            couponId = 2L,
            userId = 2L,
            issuedAt = now.minusDays(2),
            description = "이벤트 쿠폰 발급",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val expiredHistory1 = CouponIssueHistory.createExpirationHistory(
            couponId = 3L,
            userId = 3L,
            issuedAt = now.minusDays(35),
            expiredAt = now.minusDays(5),
            description = "사용 기간 만료",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        storage[issuedHistory1.id] = issuedHistory1
        storage[usedHistory1.id] = usedHistory1
        storage[issuedHistory2.id] = issuedHistory2
        storage[expiredHistory1.id] = expiredHistory1
    }

    /**
     * 쿠폰 발급 이력을 저장하거나 업데이트한다
     *
     * @param couponIssueHistory 저장할 쿠폰 발급 이력 엔티티
     * @return 저장된 쿠폰 발급 이력 엔티티 (ID가 할당된 상태)
     */
    override fun save(couponIssueHistory: CouponIssueHistory): CouponIssueHistory {
        simulateLatency()

        val savedEntity = if (couponIssueHistory.id == 0L) {
            couponIssueHistory.copy(id = idGenerator.getAndIncrement())
        } else {
            couponIssueHistory
        }
        storage[savedEntity.id] = savedEntity
        return savedEntity
    }

    /**
     * 쿠폰 발급 이력 ID로 조회한다
     *
     * @param id 조회할 쿠폰 발급 이력의 ID
     * @return 쿠폰 발급 이력 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): CouponIssueHistory? {
        simulateLatency()
        return storage[id]
    }

    /**
     * 쿠폰 ID와 사용자 ID로 쿠폰 발급 이력들을 조회한다
     *
     * @param couponId 조회할 쿠폰의 ID
     * @param userId 조회할 사용자의 ID
     * @return 조건에 맞는 쿠폰 발급 이력 목록
     */
    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): List<CouponIssueHistory> {
        simulateLatency()
        return storage.values.filter { it.couponId == couponId && it.userId == userId }
    }

    /**
     * 사용자 ID로 쿠폰 발급 이력들을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 모든 쿠폰 발급 이력 목록
     */
    override fun findByUserId(userId: Long): List<CouponIssueHistory> {
        simulateLatency()
        return storage.values.filter { it.userId == userId }
    }

    /**
     * 쿠폰 ID로 쿠폰 발급 이력들을 조회한다
     *
     * @param couponId 조회할 쿠폰의 ID
     * @return 쿠폰의 모든 발급 이력 목록
     */
    override fun findByCouponId(couponId: Long): List<CouponIssueHistory> {
        simulateLatency()
        return storage.values.filter { it.couponId == couponId }
    }

    /**
     * 쿠폰 사용 상태로 쿠폰 발급 이력들을 조회한다
     *
     * @param status 조회할 쿠폰 사용 상태
     * @return 상태에 맞는 쿠폰 발급 이력 목록
     */
    override fun findByStatus(status: UserCouponStatus): List<CouponIssueHistory> {
        simulateLatency()
        return storage.values.filter { it.status == status }
    }

    /**
     * 쿠폰 ID와 상태로 쿠폰 발급 이력 개수를 조회한다
     *
     * @param couponId 조회할 쿠폰의 ID
     * @param status 조회할 쿠폰 사용 상태
     * @return 조건에 맞는 쿠폰 발급 이력 개수
     */
    override fun countByCouponIdAndStatus(couponId: Long, status: UserCouponStatus): Long {
        simulateLatency()
        return storage.values.count { it.couponId == couponId && it.status == status }.toLong()
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
        storage.clear()
        idGenerator.set(1)
        initializeSampleData()
    }

    private fun CouponIssueHistory.copy(
        id: Long = this.id,
        couponId: Long = this.couponId,
        userId: Long = this.userId,
        status: UserCouponStatus = this.status,
        issuedAt: java.time.LocalDateTime = this.issuedAt,
        usedAt: java.time.LocalDateTime? = this.usedAt,
        expiredAt: java.time.LocalDateTime? = this.expiredAt,
        description: String? = this.description
    ): CouponIssueHistory {
        return CouponIssueHistory(id, couponId, userId, status, issuedAt, usedAt, expiredAt, description)
    }
}