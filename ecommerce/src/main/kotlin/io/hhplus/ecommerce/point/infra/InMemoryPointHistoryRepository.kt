package io.hhplus.ecommerce.point.infra

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 포인트 내역 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 포인트 거래 내역 데이터의 영속화 및 조회
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - PointHistoryRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryPointHistoryRepository : PointHistoryRepository {
    private val pointHistories = ConcurrentHashMap<Long, PointHistory>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val earn1 = PointHistory(
            id = idGenerator.getAndIncrement(),
            userId = 1L,
            amount = 100000L,
            transactionType = PointTransactionType.EARN,
            balanceBefore = 0L,
            balanceAfter = 100000L,
            description = "회원 가입 축하 포인트 적립"
        )

        val use1 = PointHistory(
            id = idGenerator.getAndIncrement(),
            userId = 1L,
            amount = -10000L,
            transactionType = PointTransactionType.USE,
            balanceBefore = 100000L,
            balanceAfter = 90000L,
            description = "주문 할인 사용"
        )

        val earn2 = PointHistory(
            id = idGenerator.getAndIncrement(),
            userId = 2L,
            amount = 50000L,
            transactionType = PointTransactionType.EARN,
            balanceBefore = 0L,
            balanceAfter = 50000L,
            description = "구매 적립 (5%)"
        )

        pointHistories[earn1.id] = earn1
        pointHistories[use1.id] = use1
        pointHistories[earn2.id] = earn2
    }

    /**
     * 포인트 내역을 저장하거나 업데이트한다
     *
     * @param pointHistory 저장할 포인트 내역 엔티티
     * @return 저장된 포인트 내역 엔티티 (ID가 할당된 상태)
     */
    override fun save(pointHistory: PointHistory): PointHistory {
        simulateLatency()

        val savedPointHistory = if (pointHistory.id == 0L) {
            // 새로운 엔티티인 경우 ID 할당
            PointHistory(
                id = idGenerator.getAndIncrement(),
                userId = pointHistory.userId,
                amount = pointHistory.amount,
                transactionType = pointHistory.transactionType,
                balanceBefore = pointHistory.balanceBefore,
                balanceAfter = pointHistory.balanceAfter,
                description = pointHistory.description
            )
        } else {
            pointHistory
        }
        pointHistories[savedPointHistory.id] = savedPointHistory
        return savedPointHistory
    }

    /**
     * 포인트 내역 ID로 조회한다
     *
     * @param id 조회할 포인트 내역의 ID
     * @return 포인트 내역 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): PointHistory? {
        simulateLatency()
        return pointHistories[id]
    }

    /**
     * 사용자 ID로 모든 포인트 내역을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 모든 포인트 내역 목록
     */
    override fun findByUserId(userId: Long): List<PointHistory> {
        simulateLatency()
        return pointHistories.values.filter { it.userId == userId }
    }

    /**
     * 사용자 ID로 포인트 내역을 생성일 내림차순으로 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 포인트 내역 목록 (생성일 내림차순 정렬)
     */
    override fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<PointHistory> {
        simulateLatency()
        return pointHistories.values
            .filter { it.userId == userId }
            .sortedByDescending { it.createdAt }
    }

    /**
     * 포인트 내역을 삭제한다
     *
     * @param pointHistory 삭제할 포인트 내역 엔티티
     */
    override fun delete(pointHistory: PointHistory) {
        simulateLatency()
        pointHistories.remove(pointHistory.id)
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
        pointHistories.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}