package io.hhplus.ecommerce.cart.application

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.common.exception.cart.CartException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 도메인 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 장바구니 도메인의 핵심 비즈니스 로직 처리
 * - 장바구니 생명주기 관리 및 상태 변경
 * - 장바구니 아이템 관리 및 차 구성 연동
 *
 * 책임:
 * - 사용자별 장바구니 생성 및 조회
 * - 장바구니 아이템 추가, 수정, 삭제 로직
 * - 장바구니 아이템의 차 구성 관리
 * - 장바구니 전체 비우기 기능
 */
@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemTeaService: CartItemTeaService
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
     * @param productId 추가할 상품 ID
     * @param boxTypeId 박스 타입 ID
     * @param quantity 추가할 수량
     * @param teaItems 차 구성 목록
     * @return 업데이트된 장바구니
     */
    @Transactional
    fun addToCart(userId: Long, productId: Long, boxTypeId: Long, quantity: Int, teaItems: List<TeaItemRequest>): Cart {
        // 차 구성 검증
        cartItemTeaService.validateTeaItems(teaItems)

        val cart = getOrCreateCart(userId)

        // 기존 동일 상품이 있는지 확인
        val existingItem = cart.items.find { it.productId == productId && it.boxTypeId == boxTypeId }

        if (existingItem != null) {
            // 기존 아이템 수량 증가
            existingItem.updateQuantity(existingItem.quantity + quantity, userId)

            // 차 구성 업데이트
            cartItemTeaService.updateCartItemTeas(existingItem.id, teaItems)
        } else {
            // 새 아이템 추가
            cart.addItem(productId, boxTypeId, quantity, userId)
            val savedCart = cartRepository.save(cart)

            // 새로 추가된 아이템의 차 구성 저장
            val newCartItem = savedCart.items.last()
            cartItemTeaService.saveCartItemTeas(newCartItem.id, teaItems)

            return savedCart
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

        // 관련된 차 구성 먼저 삭제
        cartItemTeaService.deleteCartItemTeas(cartItemId)

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

        // 모든 차 구성 삭제
        cart.items.forEach { cartItem ->
            cartItemTeaService.deleteCartItemTeas(cartItem.id)
        }

        cart.clear(userId)
        return cartRepository.save(cart)
    }

    /**
     * 장바구니 아이템의 차 구성을 조회한다
     *
     * @param cartItemId 조회할 장바구니 아이템 ID
     * @return 장바구니 아이템의 차 구성 목록
     */
    fun getCartItemTeas(cartItemId: Long): List<CartItemTea> {
        return cartItemTeaService.getCartItemTeas(cartItemId)
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