package io.hhplus.ecommerce.cart.domain.service

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.exception.CartException
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 장바구니 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 장바구니 엔티티 생성 및 상태 관리
 * - 장바구니 아이템 추가, 수정, 삭제 로직
 * - 장바구니 조회 및 생명주기 관리
 *
 * 책임:
 * - 장바구니 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - 외부 시스템 연동 로직 없음
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class CartDomainService(
    private val cartRepository: CartRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자의 장바구니를 조회하거나 없으면 새로 생성
     *
     * @param userId 사용자 ID
     * @return 기존 장바구니 또는 새로 생성된 장바구니
     */
    fun getOrCreateCart(userId: Long): Cart {
        return cartRepository.findByUserIdWithItems(userId)
            ?: cartRepository.save(Cart.create(userId = userId))
    }

    /**
     * 사용자의 장바구니 조회
     *
     * @param userId 사용자 ID
     * @return 장바구니 (없으면 null)
     */
    fun getCartByUser(userId: Long): Cart? {
        return cartRepository.findByUserIdWithItems(userId)
    }

    /**
     * 사용자의 장바구니 조회 (없으면 예외)
     *
     * @param userId 사용자 ID
     * @return 장바구니
     * @throws CartException.CartNotFound 장바구니가 없는 경우
     */
    fun getCartByUserOrThrow(userId: Long): Cart {
        return cartRepository.findByUserIdWithItems(userId)
            ?: throw CartException.CartNotFound(userId)
    }

    /**
     * 장바구니에 상품 추가
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @param quantity 수량
     * @param giftWrap 선물포장 여부
     * @param giftMessage 선물메시지
     * @return 업데이트된 장바구니
     */
    fun addToCart(
        userId: Long,
        productId: Long,
        quantity: Int,
        giftWrap: Boolean = false,
        giftMessage: String? = null
    ): Cart {
        val cart = getOrCreateCart(userId)

        // 기존 동일 상품이 있는지 확인
        val existingItem = cart.items.find { it.productId == productId }

        if (existingItem != null) {
            // 기존 아이템이 있으면 수량 및 선물 옵션 업데이트
            cart.updateItem(existingItem.id, quantity, giftWrap, giftMessage)
        } else {
            // 새 아이템 추가
            cart.addItem(
                productId = productId,
                quantity = quantity,
                giftWrap = giftWrap,
                giftMessage = giftMessage
            )
        }

        return cartRepository.save(cart)
    }

    /**
     * 장바구니 아이템 수량 업데이트
     *
     * @param userId 사용자 ID
     * @param cartItemId 장바구니 아이템 ID
     * @param quantity 새로운 수량 (0 이하이면 삭제)
     * @return 업데이트된 장바구니
     * @throws CartException.CartNotFound 장바구니가 없는 경우
     */
    fun updateCartItem(userId: Long, cartItemId: Long, quantity: Int): Cart {
        val cart = getCartByUserOrThrow(userId)

        if (quantity <= 0) {
            cart.removeItem(cartItemId)
        } else {
            cart.updateItemQuantity(cartItemId, quantity)
        }

        return cartRepository.save(cart)
    }

    /**
     * 장바구니에서 특정 아이템 삭제
     *
     * @param userId 사용자 ID
     * @param cartItemId 장바구니 아이템 ID
     * @return 업데이트된 장바구니
     * @throws CartException.CartNotFound 장바구니가 없는 경우
     */
    fun removeCartItem(userId: Long, cartItemId: Long): Cart {
        val cart = getCartByUserOrThrow(userId)
        cart.removeItem(cartItemId)
        return cartRepository.save(cart)
    }

    /**
     * 장바구니 전체 비우기
     *
     * @param userId 사용자 ID
     * @return 비워진 장바구니
     * @throws CartException.CartNotFound 장바구니가 없는 경우
     */
    fun clearCart(userId: Long): Cart {
        val cart = getCartByUserOrThrow(userId)
        cart.clear()
        return cartRepository.save(cart)
    }

    /**
     * 주문된 상품들을 장바구니에서 제거 (물리 삭제)
     *
     * @param userId 사용자 ID
     * @param orderedProductIds 주문된 상품 ID 목록
     */
    fun removeOrderedItems(userId: Long, orderedProductIds: List<Long>) {
        val cart = cartRepository.findByUserIdWithItems(userId) ?: return

        // 주문된 상품들만 장바구니에서 제거
        orderedProductIds.forEach { productId ->
            val itemToRemove = cart.items.find { it.productId == productId }
            itemToRemove?.let { cart.removeItem(it.id) }
        }

        // 장바구니에 아이템이 남아있으면 저장, 비어있으면 전체 삭제
        if (cart.isEmpty()) {
            cartRepository.deleteById(cart.id)
        } else {
            cartRepository.save(cart)
        }
    }

    /**
     * 장바구니 완전 삭제 (물리 삭제)
     * 사용자 탈퇴 등 특별한 경우에만 사용
     *
     * @param userId 사용자 ID
     * @throws CartException.CartNotFound 장바구니가 없는 경우
     */
    fun deleteCart(userId: Long) {
        val cart = getCartByUserOrThrow(userId)
        // Cart는 임시성 데이터이므로 물리 삭제
        cartRepository.deleteById(cart.id)
        logger.info("장바구니 삭제 완료: userId=$userId, cartId=${cart.id}")
    }
}
