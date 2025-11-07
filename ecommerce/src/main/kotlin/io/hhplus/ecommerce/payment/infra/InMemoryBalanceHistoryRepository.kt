package io.hhplus.ecommerce.payment.infra

import io.hhplus.ecommerce.payment.domain.entity.BalanceHistory
import io.hhplus.ecommerce.payment.domain.repository.BalanceHistoryRepository
import io.hhplus.ecommerce.payment.domain.constant.TransactionType
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 잔고 내역 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 사용자 잔고 변경 내역 데이터의 영속화 및 조회
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - BalanceHistoryRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryBalanceHistoryRepository : BalanceHistoryRepository {
    private val histories = ConcurrentHashMap<Long, BalanceHistory>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val deposit1 = BalanceHistory.create(
            userId = 1L,
            type = TransactionType.CHARGE,
            amount = 50000L,
            balanceAfter = 50000L,
            description = "초기 예치금 입금",
            createdBy = 1L
        ).let {
            BalanceHistory(
                id = idGenerator.getAndIncrement(),
                userId = it.userId,
                type = it.type,
                amount = it.amount,
                balanceAfter = it.balanceAfter,
                description = it.description
            )
        }

        val withdrawal1 = BalanceHistory.create(
            userId = 1L,
            type = TransactionType.USE,
            amount = 15000L,
            balanceAfter = 35000L,
            description = "상품 결제 차감",
            createdBy = 1L
        ).let {
            BalanceHistory(
                id = idGenerator.getAndIncrement(),
                userId = it.userId,
                type = it.type,
                amount = it.amount,
                balanceAfter = it.balanceAfter,
                description = it.description
            )
        }

        val deposit2 = BalanceHistory.create(
            userId = 2L,
            type = TransactionType.CHARGE,
            amount = 30000L,
            balanceAfter = 30000L,
            description = "포인트 충전",
            createdBy = 2L
        ).let {
            BalanceHistory(
                id = idGenerator.getAndIncrement(),
                userId = it.userId,
                type = it.type,
                amount = it.amount,
                balanceAfter = it.balanceAfter,
                description = it.description
            )
        }

        histories[deposit1.id] = deposit1
        histories[withdrawal1.id] = withdrawal1
        histories[deposit2.id] = deposit2
    }

    /**
     * 잔고 내역을 저장하거나 업데이트한다
     *
     * @param balanceHistory 저장할 잔고 내역 엔티티
     * @return 저장된 잔고 내역 엔티티 (ID가 할당된 상태)
     */
    override fun save(balanceHistory: BalanceHistory): BalanceHistory {
        simulateLatency()

        val savedHistory = if (balanceHistory.id == 0L) {
            BalanceHistory(
                id = idGenerator.getAndIncrement(),
                userId = balanceHistory.userId,
                type = balanceHistory.type,
                amount = balanceHistory.amount,
                balanceAfter = balanceHistory.balanceAfter,
                description = balanceHistory.description
            )
        } else {
            balanceHistory
        }

        histories[savedHistory.id] = savedHistory
        return savedHistory
    }

    /**
     * 잔고 내역 ID로 조회한다
     *
     * @param id 조회할 잔고 내역의 ID
     * @return 잔고 내역 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): BalanceHistory? {
        simulateLatency()
        return histories[id]
    }

    /**
     * 사용자 ID로 모든 잔고 내역을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 모든 잔고 내역 목록 (생성일 내림차순 정렬)
     */
    override fun findByUserId(userId: Long): List<BalanceHistory> {
        simulateLatency()
        return histories.values.filter { it.userId == userId }
            .sortedByDescending { it.createdAt }
    }

    /**
     * 사용자 ID와 거래 타입으로 잔고 내역을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param type 조회할 거래 타입
     * @return 조건에 맞는 잔고 내역 목록 (생성일 내림차순 정렬)
     */
    override fun findByUserIdAndType(userId: Long, type: TransactionType): List<BalanceHistory> {
        simulateLatency()
        return histories.values.filter { it.userId == userId && it.type == type }
            .sortedByDescending { it.createdAt }
    }

    /**
     * 잔고 내역을 삭제한다
     *
     * @param balanceHistory 삭제할 잔고 내역 엔티티
     */
    override fun delete(balanceHistory: BalanceHistory) {
        simulateLatency()
        histories.remove(balanceHistory.id)
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
        histories.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}