package io.hhplus.ecommerce.cart.infra

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.domain.repository.CartItemRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * 장바구니 아이템 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 장바구니 아이템 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - CartItemRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 지연시간 시뮤레이션을 통한 실제 DB 환경 모사
 */
@Repository
class InMemoryCartItemRepository : CartItemRepository {
    private val cartItems = ConcurrentHashMap<Long, CartItem>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val cartItem1 = CartItem(
            id = idGenerator.getAndIncrement(),
            cartId = 1L,
            packageTypeId = 1L, // THREE_DAYS 패키지
            packageTypeName = "3일 패키지",
            packageTypeDays = 3,
            dailyServing = 1,
            totalQuantity = 30.0, // 그램
            giftWrap = false,
            giftMessage = null
        )

        val cartItem2 = CartItem(
            id = idGenerator.getAndIncrement(),
            cartId = 1L,
            packageTypeId = 2L, // SEVEN_DAYS 패키지
            packageTypeName = "7일 패키지",
            packageTypeDays = 7,
            dailyServing = 2,
            totalQuantity = 140.0, // 그램
            giftWrap = true,
            giftMessage = "건강한 차생활을 위해"
        )

        cartItems[cartItem1.id] = cartItem1
        cartItems[cartItem2.id] = cartItem2
    }

    /**
     * 장바구니 아이템을 저장하거나 업데이트한다
     *
     * @param cartItem 저장할 장바구니 아이템 엔티티
     * @return 저장된 장바구니 아이템 엔티티 (ID가 할당된 상태)
     */
    override fun save(cartItem: CartItem): CartItem {
        simulateLatency()

        val savedCartItem = if (cartItem.id == 0L) {
            CartItem(
                id = idGenerator.getAndIncrement(),
                cartId = cartItem.cartId,
                packageTypeId = cartItem.packageTypeId,
                packageTypeName = cartItem.packageTypeName,
                packageTypeDays = cartItem.packageTypeDays,
                dailyServing = cartItem.dailyServing,
                totalQuantity = cartItem.totalQuantity,
                giftWrap = cartItem.giftWrap,
                giftMessage = cartItem.giftMessage
            )
        } else {
            cartItem
        }

        cartItems[savedCartItem.id] = savedCartItem
        return savedCartItem
    }

    /**
     * 장바구니 아이템 ID로 장바구니 아이템을 조회한다
     *
     * @param id 조회할 장바구니 아이템의 ID
     * @return 장바구니 아이템 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): CartItem? {
        simulateLatency()
        return cartItems[id]
    }

    /**
     * 장바구니 ID로 모든 장바구니 아이템을 조회한다
     *
     * @param cartId 조회할 장바구니의 ID
     * @return 장바구니에 속한 아이템 목록
     */
    override fun findByCartId(cartId: Long): List<CartItem> {
        simulateLatency()
        return cartItems.values.filter { it.cartId == cartId }
    }

    /**
     * 장바구니 ID와 제품 ID로 장바구니 아이템을 조회한다
     *
     * @param cartId 조회할 장바구니의 ID
     * @param productId 조회할 제품의 ID
     * @return 조건에 맞는 장바구니 아이템 (존재하지 않을 경우 null)
     */
    override fun findByCartIdAndProductId(cartId: Long, productId: Long): CartItem? {
        simulateLatency()
        // 새로운 구조에서는 productId 개념이 없으므로 packageTypeId로 대체
        return cartItems.values.find { it.cartId == cartId && it.packageTypeId == productId }
    }

    /**
     * 장바구니 ID, 제품 ID, 박스 타입 ID로 장바구니 아이템을 조회한다
     *
     * @param cartId 조회할 장바구니의 ID
     * @param productId 조회할 제품의 ID (packageTypeId로 사용)
     * @param boxTypeId 조회할 박스 타입의 ID (packageTypeId로 사용)
     * @return 조건에 맞는 장바구니 아이템 (존재하지 않을 경우 null)
     */
    override fun findByCartIdAndProductIdAndBoxTypeId(cartId: Long, productId: Long, boxTypeId: Long): CartItem? {
        simulateLatency()
        return cartItems.values.find {
            it.cartId == cartId && it.packageTypeId == boxTypeId
        }
    }

    /**
     * 장바구니 아이템 ID로 장바구니 아이템을 삭제한다
     *
     * @param id 삭제할 장바구니 아이템의 ID
     */
    override fun deleteById(id: Long) {
        simulateLatency()
        cartItems.remove(id)
    }

    /**
     * 장바구니 ID로 모든 장바구니 아이템을 삭제한다
     *
     * @param cartId 삭제할 장바구니의 ID
     */
    override fun deleteByCartId(cartId: Long) {
        simulateLatency()
        cartItems.values.removeAll { it.cartId == cartId }
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
        cartItems.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}