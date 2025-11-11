package io.hhplus.ecommerce.order.infra

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 주문 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 주문 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - OrderRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 테스트용 샘플 데이터 초기화
 */
@Repository
class InMemoryOrderRepository : OrderRepository {

    private val orders = ConcurrentHashMap<Long, Order>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        // Sample Order 1
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        //
        // - Completed order
        val order1 = Order(
            id = idGenerator.getAndIncrement(),
            orderNumber = "ORDER-001-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
            userId = 1L,
            totalAmount = 45000L,
            discountAmount = 5000L,
            finalAmount = 40000L,
            usedCouponId = 1L,
            status = OrderStatus.COMPLETED
        )

        // Sample Order 2 - Pending order
        val order2 = Order(
            id = idGenerator.getAndIncrement(),
            orderNumber = "ORDER-002-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
            userId = 2L,
            totalAmount = 25000L,
            discountAmount = 0L,
            finalAmount = 25000L,
            usedCouponId = null,
            status = OrderStatus.PENDING
        )

        // Sample Order 3 - Confirmed order
        val order3 = Order(
            id = idGenerator.getAndIncrement(),
            orderNumber = "ORDER-003-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
            userId = 1L,
            totalAmount = 60000L,
            discountAmount = 10000L,
            finalAmount = 50000L,
            usedCouponId = 2L,
            status = OrderStatus.CONFIRMED
        )

        orders[order1.id] = order1
        orders[order2.id] = order2
        orders[order3.id] = order3
    }


    /**
     * 주문을 저장하거나 업데이트한다
     *
     * @param order 저장할 주문 엔티티
     * @return 저장된 주문 엔티티 (ID가 할당된 상태)
     */
    override fun save(order: Order): Order {
        simulateLatency()

        val savedOrder = if (order.id == 0L) {
            Order(
                id = idGenerator.getAndIncrement(),
                orderNumber = order.orderNumber,
                userId = order.userId,
                totalAmount = order.totalAmount,
                discountAmount = order.discountAmount,
                finalAmount = order.finalAmount,
                usedCouponId = order.usedCouponId,
                status = order.status
            )
        } else {
            order
        }

        orders[savedOrder.id] = savedOrder
        return savedOrder
    }

    /**
     * 주문 ID로 주문을 조회한다
     *
     * @param id 조회할 주문의 ID
     * @return 주문 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): Order? {
        simulateLatency()
        return orders[id]
    }

    /**
     * 주문 번호로 주문을 조회한다
     *
     * @param orderNumber 조회할 주문 번호
     * @return 주문 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByOrderNumber(orderNumber: String): Order? {
        simulateLatency()
        return orders.values.find { it.orderNumber == orderNumber }
    }

    /**
     * 사용자 ID로 모든 주문을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 주문 목록
     */
    override fun findByUserId(userId: Long): List<Order> {
        simulateLatency()
        return orders.values.filter { it.userId == userId }
    }

    /**
     * 사용자 ID와 활성 상태로 주문을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param isActive 조회할 주문의 활성 상태
     * @return 조건에 맞는 주문 목록
     */
    override fun findByUserIdAndIsActive(userId: Long, isActive: Boolean): List<Order> {
        simulateLatency()
        return orders.values.filter { it.userId == userId && it.isActive == isActive }
    }

    /**
     * 사용자 ID와 주문 상태로 주문을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param status 조회할 주문 상태
     * @return 조건에 맞는 주문 목록
     */
    override fun findByUserIdAndStatus(userId: Long, status: OrderStatus): List<Order> {
        simulateLatency()
        return orders.values.filter { it.userId == userId && it.status == status }
    }

    /**
     * 주문 상태로 주문을 조회한다
     *
     * @param status 조회할 주문 상태
     * @return 상태에 맞는 주문 목록
     */
    override fun findByStatus(status: OrderStatus): List<Order> {
        simulateLatency()
        return orders.values.filter { it.status == status }
    }

    /**
     * 생성일 범위로 주문을 조회한다
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 날짜 범위에 맞는 주문 목록
     */
    override fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Order> {
        simulateLatency()
        return orders.values.filter {
            it.createdAt.isAfter(startDate.minusSeconds(1)) &&
            it.createdAt.isBefore(endDate.plusSeconds(1))
        }
    }

    /**
     * 사용자 ID와 주문 상태로 주문 개수를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @param status 조회할 주문 상태
     * @return 조건에 맞는 주문 개수
     */
    override fun countByUserIdAndStatus(userId: Long, status: OrderStatus): Long {
        simulateLatency()
        return orders.values.count { it.userId == userId && it.status == status }.toLong()
    }

    /**
     * 실제 데이터베이스 지연시간을 시뮬레이션한다
     */
    private fun simulateLatency() {
        Thread.sleep(kotlin.random.Random.nextLong(50, 200))
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        orders.clear()
        idGenerator.set(1)
        initializeSampleData()
    }

    /**
     * 저장된 주문 개수를 반환한다 (테스트 전용)
     *
     * @return 저장된 주문 개수
     */
    fun size(): Int = orders.size
}