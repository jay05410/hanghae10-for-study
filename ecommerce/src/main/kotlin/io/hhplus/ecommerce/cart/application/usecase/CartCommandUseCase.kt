package io.hhplus.ecommerce.cart.application.usecase

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import io.hhplus.ecommerce.cart.presentation.dto.AddToCartRequest
import io.hhplus.ecommerce.product.domain.service.ProductDomainService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 트랜잭션 경계 관리
 * - 장바구니 변경 작업 오케스트레이션
 * - 외부 도메인 서비스 협력 조정
 *
 * 책임:
 * - 장바구니 아이템 추가, 수정, 삭제 기능 제공
 * - 상품 유효성 검증 (ProductDomainService 호출)
 * - 장바구니 데이터 무결성 보장
 */
@Component
class CartCommandUseCase(
    private val cartDomainService: CartDomainService,
    private val productDomainService: ProductDomainService
) {

    /**
     * 장바구니에 상품 추가
     *
     * @param userId 인증된 사용자 ID
     * @param request 장바구니 추가 요청 데이터
     * @return 상품이 추가된 업데이트된 장바구니
     * @throws IllegalArgumentException 유효하지 않은 상품 ID인 경우
     */
    @Transactional
    fun addToCart(userId: Long, request: AddToCartRequest): Cart {
        // 1. 상품 존재 여부 검증
        productDomainService.getProduct(request.productId)

        // 2. 장바구니에 상품 추가
        return cartDomainService.addToCart(
            userId = userId,
            productId = request.productId,
            quantity = request.quantity,
            giftWrap = request.giftWrap,
            giftMessage = request.giftMessage
        )
    }

    /**
     * 장바구니 내 지정된 아이템의 수량 수정
     *
     * @param userId 인증된 사용자 ID
     * @param cartItemId 수정할 장바구니 아이템 ID
     * @param quantity 변경할 새로운 수량 (양수)
     * @return 수량이 업데이트된 장바구니
     * @throws IllegalArgumentException 사용자가 소유하지 않은 아이템이거나 잘못된 수량인 경우
     */
    @Transactional
    fun updateCartItem(userId: Long, cartItemId: Long, quantity: Int): Cart {
        return cartDomainService.updateCartItem(
            userId = userId,
            cartItemId = cartItemId,
            quantity = quantity
        )
    }

    /**
     * 장바구니에서 지정된 아이템 제거
     *
     * @param userId 인증된 사용자 ID
     * @param cartItemId 제거할 장바구니 아이템 ID
     * @return 아이템이 제거된 업데이트된 장바구니
     * @throws IllegalArgumentException 사용자가 소유하지 않은 아이템이거나 존재하지 않는 아이템인 경우
     */
    @Transactional
    fun removeCartItem(userId: Long, cartItemId: Long): Cart {
        return cartDomainService.removeCartItem(userId, cartItemId)
    }

    /**
     * 사용자의 장바구니 전체 비우기
     *
     * @param userId 인증된 사용자 ID
     * @return 모든 아이템이 제거된 비어있는 장바구니
     */
    @Transactional
    fun clearCart(userId: Long): Cart {
        return cartDomainService.clearCart(userId)
    }

    /**
     * 주문된 상품들을 장바구니에서 제거
     *
     * @param userId 인증된 사용자 ID
     * @param orderedProductIds 주문된 상품 ID 목록
     */
    @Transactional
    fun removeOrderedItems(userId: Long, orderedProductIds: List<Long>) {
        cartDomainService.removeOrderedItems(userId, orderedProductIds)
    }

    /**
     * 사용자의 장바구니 완전 삭제 (물리 삭제)
     * 사용자 탈퇴 등 특별한 경우에만 사용
     *
     * @param userId 인증된 사용자 ID
     */
    @Transactional
    fun deleteCartCompletely(userId: Long) {
        cartDomainService.deleteCart(userId)
    }
}
