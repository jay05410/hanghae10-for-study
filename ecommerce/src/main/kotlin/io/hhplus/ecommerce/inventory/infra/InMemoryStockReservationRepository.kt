package io.hhplus.ecommerce.inventory.infra

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 재고 예약 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 재고 예약 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - StockReservationRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryStockReservationRepository : StockReservationRepository {
    private val reservations = ConcurrentHashMap<Long, StockReservation>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        val activeReservation = StockReservation.create(
            productId = 1L,
            userId = 1L,
            quantity = 2,
            reservationMinutes = 20,
            createdBy = 1L
        ).let {
            StockReservation(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                userId = it.userId,
                quantity = it.quantity,
                status = ReservationStatus.RESERVED,
                reservedAt = now.minusMinutes(5),
                expiresAt = now.plusMinutes(15),
                version = it.version
            )
        }

        val expiredReservation = StockReservation.create(
            productId = 2L,
            userId = 1L,
            quantity = 1,
            reservationMinutes = 20,
            createdBy = 1L
        ).let {
            StockReservation(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                userId = it.userId,
                quantity = it.quantity,
                status = ReservationStatus.EXPIRED,
                reservedAt = now.minusMinutes(30),
                expiresAt = now.minusMinutes(10),
                version = it.version
            )
        }

        val confirmedReservation = StockReservation.create(
            productId = 3L,
            userId = 2L,
            quantity = 3,
            reservationMinutes = 20,
            createdBy = 2L
        ).let {
            StockReservation(
                id = idGenerator.getAndIncrement(),
                productId = it.productId,
                userId = it.userId,
                quantity = it.quantity,
                status = ReservationStatus.CONFIRMED,
                reservedAt = now.minusHours(1),
                expiresAt = now.minusMinutes(40),
                version = it.version
            )
        }

        reservations[activeReservation.id] = activeReservation
        reservations[expiredReservation.id] = expiredReservation
        reservations[confirmedReservation.id] = confirmedReservation
    }

    /**
     * 재고 예약 정보를 저장하거나 업데이트한다
     *
     * @param stockReservation 저장할 재고 예약 엔티티
     * @return 저장된 재고 예약 엔티티 (ID가 할당된 상태)
     */
    override fun save(stockReservation: StockReservation): StockReservation {
        simulateLatency()

        val savedReservation = if (stockReservation.id == 0L) {
            StockReservation(
                id = idGenerator.getAndIncrement(),
                productId = stockReservation.productId,
                userId = stockReservation.userId,
                quantity = stockReservation.quantity,
                status = stockReservation.status,
                reservedAt = stockReservation.reservedAt,
                expiresAt = stockReservation.expiresAt,
                version = stockReservation.version
            )
        } else {
            stockReservation
        }
        reservations[savedReservation.id] = savedReservation
        return savedReservation
    }

    /**
     * 재고 예약 ID로 조회한다
     *
     * @param id 조회할 재고 예약의 ID
     * @return 재고 예약 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): StockReservation? {
        simulateLatency()
        return reservations[id]
    }

    /**
     * 사용자 ID, 제품 ID, 상태로 재고 예약을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param productId 조회할 제품의 ID
     * @param status 조회할 예약 상태
     * @return 조건에 맞는 재고 예약 (존재하지 않을 경우 null)
     */
    override fun findByUserIdAndProductIdAndStatus(
        userId: Long,
        productId: Long,
        status: ReservationStatus
    ): StockReservation? {
        simulateLatency()
        return reservations.values.find {
            it.userId == userId && it.productId == productId && it.status == status
        }
    }

    /**
     * 지정된 시간 이전에 만료된 예약들을 조회한다
     *
     * @param expiredBefore 만료 기준 시간
     * @return 만료된 재고 예약 목록
     */
    override fun findExpiredReservations(expiredBefore: LocalDateTime): List<StockReservation> {
        simulateLatency()
        return reservations.values.filter {
            it.status == ReservationStatus.RESERVED && it.expiresAt.isBefore(expiredBefore)
        }
    }

    /**
     * 사용자 ID와 상태로 재고 예약들을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param status 조회할 예약 상태
     * @return 조건에 맞는 재고 예약 목록
     */
    override fun findByUserIdAndStatus(userId: Long, status: ReservationStatus): List<StockReservation> {
        simulateLatency()
        return reservations.values.filter {
            it.userId == userId && it.status == status
        }
    }

    /**
     * 재고 예약을 삭제한다
     *
     * @param stockReservation 삭제할 재고 예약 엔티티
     */
    override fun delete(stockReservation: StockReservation) {
        simulateLatency()
        reservations.remove(stockReservation.id)
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
        reservations.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}