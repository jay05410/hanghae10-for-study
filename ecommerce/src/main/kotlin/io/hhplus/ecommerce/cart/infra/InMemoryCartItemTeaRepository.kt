package io.hhplus.ecommerce.cart.infra

import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import io.hhplus.ecommerce.cart.domain.repository.CartItemTeaRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 장바구니 아이템 차 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 장바구니 아이템별 차 정보 데이터의 영속화 및 조회
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - CartItemTeaRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryCartItemTeaRepository : CartItemTeaRepository {
    private val storage = ConcurrentHashMap<Long, CartItemTea>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val cartItemTea1 = CartItemTea.create(
            cartItemId = 1L,
            productId = 1L, // 녹차 상품
            quantity = 2
        ).copy(id = idGenerator.getAndIncrement())

        val cartItemTea2 = CartItemTea.create(
            cartItemId = 1L,
            productId = 2L, // 홍차 상품
            quantity = 1
        ).copy(id = idGenerator.getAndIncrement())

        val cartItemTea3 = CartItemTea.create(
            cartItemId = 2L,
            productId = 3L, // 허브차 상품
            quantity = 3
        ).copy(id = idGenerator.getAndIncrement())

        storage[cartItemTea1.id] = cartItemTea1
        storage[cartItemTea2.id] = cartItemTea2
        storage[cartItemTea3.id] = cartItemTea3
    }

    /**
     * 장바구니 아이템 차 정보를 저장하거나 업데이트한다
     *
     * @param cartItemTea 저장할 장바구니 아이템 차 정보 엔티티
     * @return 저장된 장바구니 아이템 차 정보 엔티티 (ID가 할당된 상태)
     */
    override fun save(cartItemTea: CartItemTea): CartItemTea {
        simulateLatency()

        val savedEntity = if (cartItemTea.id == 0L) {
            cartItemTea.copy(id = idGenerator.getAndIncrement())
        } else {
            cartItemTea
        }
        storage[savedEntity.id] = savedEntity
        return savedEntity
    }

    /**
     * 장바구니 아이템 차 정보 ID로 조회한다
     *
     * @param id 조회할 장바구니 아이템 차 정보의 ID
     * @return 장바구니 아이템 차 정보 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): CartItemTea? {
        simulateLatency()
        return storage[id]
    }

    /**
     * 장바구니 아이템 ID로 연관된 모든 차 정보를 조회한다
     *
     * @param cartItemId 조회할 장바구니 아이템의 ID
     * @return 장바구니 아이템에 속한 모든 차 정보 목록
     */
    override fun findByCartItemId(cartItemId: Long): List<CartItemTea> {
        simulateLatency()
        return storage.values.filter { it.cartItemId == cartItemId }
    }

    /**
     * 장바구니 아이템 차 정보 ID로 삭제한다
     *
     * @param id 삭제할 장바구니 아이템 차 정보의 ID
     */
    override fun deleteById(id: Long) {
        simulateLatency()
        storage.remove(id)
    }

    /**
     * 장바구니 아이템 ID로 모든 연관된 차 정보를 삭제한다
     *
     * @param cartItemId 삭제할 장바구니 아이템의 ID
     */
    override fun deleteByCartItemId(cartItemId: Long) {
        simulateLatency()
        storage.values.removeIf { it.cartItemId == cartItemId }
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

    private fun CartItemTea.copy(
        id: Long = this.id,
        cartItemId: Long = this.cartItemId,
        productId: Long = this.productId,
        quantity: Int = this.quantity
    ): CartItemTea {
        return CartItemTea(id, cartItemId, productId, quantity)
    }
}