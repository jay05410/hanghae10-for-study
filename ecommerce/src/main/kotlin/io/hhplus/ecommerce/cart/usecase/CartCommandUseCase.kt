package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.product.application.ProductService
import org.springframework.stereotype.Component

/**
 * 장바구니 명령 UseCase
 *
 * 역할:
 * - 모든 장바구니 변경 작업을 통합 관리
 * - 장바구니 아이템 추가, 수정, 삭제, 전체 비우기 기능 제공
 *
 * 책임:
 * - 장바구니 변경 요청 검증 및 실행
 * - 장바구니 데이터 무결성 보장
 */
@Component
class CartCommandUseCase(
    private val cartService: CartService,
    private val productService: ProductService
) {

    /**
     * 장바구니에 상품을 추가한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 장바구니 추가 요청 데이터
     * @return 상품이 추가된 업데이트된 장바구니
     * @throws IllegalArgumentException 유효하지 않은 상품 ID인 경우
     */
    fun addToCart(userId: Long, request: AddToCartRequest): Cart {
        // 1. 상품 존재 여부 검증
        productService.getProduct(request.productId)

        // 2. 장바구니에 상품 추가
        return cartService.addToCart(
            userId = userId,
            productId = request.productId,
            quantity = request.quantity,
            giftWrap = request.giftWrap,
            giftMessage = request.giftMessage
        )
    }

    /**
     * 장바구니 내 지정된 아이템의 수량을 수정한다
     *
     * @param userId 인증된 사용자 ID
     * @param cartItemId 수정할 장바구니 아이템 ID
     * @param quantity 변경할 새로운 수량 (양수)
     * @return 수량이 업데이트된 장바구니
     * @throws IllegalArgumentException 사용자가 소유하지 않은 아이템이거나 잘못된 수량인 경우
     */
    fun updateCartItem(userId: Long, cartItemId: Long, quantity: Int): Cart {
        return cartService.updateCartItem(
            userId = userId,
            cartItemId = cartItemId,
            quantity = quantity
        )
    }

    /**
     * 장바구니에서 지정된 아이템을 제거한다
     *
     * @param userId 인증된 사용자 ID
     * @param cartItemId 제거할 장바구니 아이템 ID
     * @return 아이템이 제거된 업데이트된 장바구니
     * @throws IllegalArgumentException 사용자가 소유하지 않은 아이템이거나 존재하지 않는 아이템인 경우
     */
    fun removeCartItem(userId: Long, cartItemId: Long): Cart {
        return cartService.removeCartItem(userId, cartItemId)
    }

    /**
     * 사용자의 장바구니를 완전히 비운다
     *
     * @param userId 인증된 사용자 ID
     * @return 모든 아이템이 제거된 비어있는 장바구니
     */
    fun clearCart(userId: Long): Cart {
        return cartService.clearCart(userId)
    }

    /**
     * 주문된 상품들을 장바구니에서 제거한다
     *
     * @param userId 인증된 사용자 ID
     * @param orderedProductIds 주문된 상품 ID 목록
     */
    fun removeOrderedItems(userId: Long, orderedProductIds: List<Long>) {
        cartService.removeOrderedItems(userId, orderedProductIds)
    }

    /**
     * 사용자의 장바구니를 완전히 삭제한다 (물리 삭제)
     * 사용자 탈퇴 등 특별한 경우에만 사용
     *
     * @param userId 인증된 사용자 ID
     */
    fun deleteCartCompletely(userId: Long) {
        cartService.deleteCart(userId)
    }
}