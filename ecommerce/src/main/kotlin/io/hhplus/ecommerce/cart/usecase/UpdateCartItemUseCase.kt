package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import org.springframework.stereotype.Component

/**
 * 장바구니 아이템 수량 수정 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 장바구니 내 아이템 수량 변경 비즈니스 플로우 수행
 * - 수량 변경 전 비즈니스 규칙 검증
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 아이템 소유권 및 수량 유효성 검증
 * - 장바구니 상태 변경 트랜잭션 관리
 * - 수량 변경 후 장바구니 상태 일관성 보장
 */
@Component
class UpdateCartItemUseCase(
    private val cartService: CartService
) {

    /**
     * 장바구니 내 지정된 아이템의 수량을 수정한다
     *
     * @param userId 인증된 사용자 ID
     * @param cartItemId 수정할 장바구니 아이템 ID
     * @param quantity 변경할 새로운 수량 (양수)
     * @return 수량이 업데이트된 장바구니
     * @throws IllegalArgumentException 사용자가 소유하지 않은 아이템이거나 잘못된 수량인 경우
     */
    fun execute(userId: Long, cartItemId: Long, quantity: Int): Cart {
        return cartService.updateCartItem(
            userId = userId,
            cartItemId = cartItemId,
            totalQuantity = quantity.toDouble(),
            updatedBy = userId
        )
    }
}