package io.hhplus.ecommerce.cart.application

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.common.exception.cart.CartException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 도메인 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 장바구니 도메인의 핵심 비즈니스 로직 처리
 * - 장바구니 생명주기 관리 및 상태 변경
 * - 장바구니 아이템 관리
 *
 * 책임:
 * - 사용자별 장바구니 생성 및 조회
 * - 장바구니 아이템 추가, 수정, 삭제 로직
 * - 장바구니 전체 비우기 기능
 */
@Service
class CartService(
    private val cartRepository: CartRepository
) {

    /**
     * 사용자의 장바구니를 조회하거나 없으면 새로 생성한다
     *
     * @param userId 장바구니를 조회할 사용자 ID
     * @return 기존 장바구니 또는 새로 생성된 장바구니
     */
    fun getOrCreateCart(userId: Long): Cart {
        return cartRepository.findByUserId(userId)
            ?: cartRepository.save(Cart.create(userId = userId, createdBy = userId))
    }

    /**
     * 장바구니에 상품을 추가한다
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @param quantity 수량
     * @param giftWrap 선물포장 여부
     * @param giftMessage 선물메시지
     * @return 업데이트된 장바구니
     */
    @Transactional
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
            // 기존 아이템이 있으면 수량 업데이트
            cart.updateItemQuantity(existingItem.id, quantity, userId)
        } else {
            // 새 아이템 추가
            cart.addItem(
                productId = productId,
                quantity = quantity,
                giftWrap = giftWrap,
                giftMessage = giftMessage,
                addedBy = userId
            )
        }

        return cartRepository.save(cart)
    }

    /**
     * 장바구니 아이템의 수량을 업데이트한다
     *
     * @param userId 사용자 ID
     * @param cartItemId 업데이트할 장바구니 아이템 ID
     * @param quantity 새로운 수량 (0 이하이면 아이템 삭제)
     * @param updatedBy 업데이트 실행자 ID
     * @return 업데이트된 장바구니
     * @throws CartException.CartNotFound 장바구니를 찾을 수 없는 경우
     */
    fun updateCartItem(userId: Long, cartItemId: Long, quantity: Int, updatedBy: Long): Cart {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CartException.CartNotFound(userId)

        if (quantity <= 0) {
            cart.removeItem(cartItemId, updatedBy)
        } else {
            cart.updateItemQuantity(cartItemId, quantity, updatedBy)
        }

        return cartRepository.save(cart)
    }

    /**
     * 장바구니에서 특정 아이템을 삭제한다
     *
     * @param userId 사용자 ID
     * @param cartItemId 삭제할 장바구니 아이템 ID
     * @return 업데이트된 장바구니
     * @throws CartException.CartNotFound 장바구니를 찾을 수 없는 경우
     */
    @Transactional
    fun removeCartItem(userId: Long, cartItemId: Long): Cart {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CartException.CartNotFound(userId)

        cart.removeItem(cartItemId, userId)
        return cartRepository.save(cart)
    }

    /**
     * 사용자의 장바구니를 전체 비운다
     *
     * @param userId 사용자 ID
     * @return 비워진 장바구니
     * @throws CartException.CartNotFound 장바구니를 찾을 수 없는 경우
     */
    @Transactional
    fun clearCart(userId: Long): Cart {
        val cart = cartRepository.findByUserId(userId)
            ?: throw CartException.CartNotFound(userId)

        cart.clear(userId)
        return cartRepository.save(cart)
    }

    /**
     * 사용자의 장바구니를 조회한다
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 장바구니 (존재하지 않을 경우 null)
     */
    fun getCartByUser(userId: Long): Cart? {
        return cartRepository.findByUserId(userId)
    }
}