package io.hhplus.ecommerce.cart.infra

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 장바구니 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 장바구니 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - CartRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 테스트용 샘플 데이터 초기화
 */
@Repository
class InMemoryCartRepository : CartRepository {
    private val carts = ConcurrentHashMap<Long, Cart>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val cart1 = Cart(
            id = 1L,
            userId = 1L
        )
        val cart2 = Cart(
            id = 2L,
            userId = 2L
        )
        val cart3 = Cart(
            id = 3L,
            userId = 3L
        )

        carts[1L] = cart1
        carts[2L] = cart2
        carts[3L] = cart3

        idGenerator.set(4L)
    }

    /**
     * 장바구니를 저장하거나 업데이트한다
     *
     * @param cart 저장할 장바구니 엔티티
     * @return 저장된 장바구니 엔티티 (ID가 할당된 상태)
     */
    override fun save(cart: Cart): Cart {
        val savedCart = if (cart.id == 0L) {
            Cart(
                id = idGenerator.getAndIncrement(),
                userId = cart.userId
            )
        } else {
            cart
        }
        carts[savedCart.id] = savedCart
        return savedCart
    }

    /**
     * 장바구니 ID로 활성 상태의 장바구니를 조회한다
     *
     * @param id 조회할 장바구니의 ID
     * @return 장바구니 엔티티 (존재하지 않거나 비활성 상태일 경우 null)
     */
    override fun findById(id: Long): Cart? {
        return carts[id]?.takeIf { it.isActive }
    }

    /**
     * 사용자 ID로 활성 상태의 장바구니를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 활성 장바구니 (존재하지 않을 경우 null)
     */
    override fun findByUserId(userId: Long): Cart? {
        return carts.values.find { it.userId == userId && it.isActive }
    }

    /**
     * 사용자 ID로 장바구니 아이템과 함께 장바구니를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 활성 장바구니 (존재하지 않을 경우 null)
     */
    override fun findByUserIdWithItems(userId: Long): Cart? {
        // InMemory 구현에서는 findByUserId와 동일하게 처리 (실제 JPA에서는 fetch join 사용)
        return findByUserId(userId)
    }

    /**
     * 장바구니를 삭제한다
     *
     * @param cart 삭제할 장바구니 엔티티
     */
    override fun delete(cart: Cart) {
        carts.remove(cart.id)
    }

    /**
     * 장바구니 ID로 장바구니를 삭제한다
     *
     * @param id 삭제할 장바구니의 ID
     */
    override fun deleteById(id: Long) {
        carts.remove(id)
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        carts.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}