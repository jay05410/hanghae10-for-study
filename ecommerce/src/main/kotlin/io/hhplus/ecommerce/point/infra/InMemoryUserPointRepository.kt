package io.hhplus.ecommerce.point.infra

import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.Balance
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 사용자 포인트 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 사용자 포인트 잔액 데이터의 영속화 및 조회
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - UserPointRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 테스트용 샘플 포인트 데이터 초기화
 */
@Repository
class InMemoryUserPointRepository : UserPointRepository {
    private val userPoints = ConcurrentHashMap<Long, UserPoint>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val userPoint1 = UserPoint(
            id = 1L,
            userId = 1L,
            balance = Balance.of(100000L), // 100,000 points
            version = 1
        )
        val userPoint2 = UserPoint(
            id = 2L,
            userId = 2L,
            balance = Balance.of(50000L), // 50,000 points
            version = 1
        )
        val userPoint3 = UserPoint(
            id = 3L,
            userId = 3L,
            balance = Balance.zero(), // No points
            version = 1
        )

        userPoints[1L] = userPoint1
        userPoints[2L] = userPoint2
        userPoints[3L] = userPoint3

        idGenerator.set(4L)
    }

    /**
     * 사용자 포인트 정보를 저장하거나 업데이트한다
     *
     * @param userPoint 저장할 사용자 포인트 엔티티
     * @return 저장된 사용자 포인트 엔티티 (ID가 할당된 상태)
     */
    override fun save(userPoint: UserPoint): UserPoint {
        val savedUserPoint = if (userPoint.id == 0L) {
            // 새로운 엔티티인 경우 ID 할당 (JPA auto increment 시뮬레이션)
            UserPoint(
                id = idGenerator.getAndIncrement(),
                userId = userPoint.userId,
                balance = userPoint.balance,
                version = userPoint.version
            )
        } else {
            userPoint
        }
        userPoints[savedUserPoint.id] = savedUserPoint
        return savedUserPoint
    }

    /**
     * 사용자 포인트 ID로 조회한다
     *
     * @param id 조회할 사용자 포인트의 ID
     * @return 사용자 포인트 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): UserPoint? {
        return userPoints[id]
    }

    /**
     * 사용자 ID로 사용자 포인트를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 포인트 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByUserId(userId: Long): UserPoint? {
        return userPoints.values.find { it.userId == userId }
    }

    /**
     * 사용자 ID로 사용자 포인트를 업데이트 락과 함께 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 포인트 엔티티 (인메모리 구현에서는 락 대신 동일 결과 반환)
     */
    override fun findByUserIdWithLock(userId: Long): UserPoint? {
        // In-memory implementation에서는 별도의 락 구현 없이 동일한 결과 반환
        return findByUserId(userId)
    }

    /**
     * 사용자 포인트 정보를 삭제한다
     *
     * @param userPoint 삭제할 사용자 포인트 엔티티
     */
    override fun delete(userPoint: UserPoint) {
        userPoints.remove(userPoint.id)
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        userPoints.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}