package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import org.springframework.stereotype.Component

/**
 * 장바구니 아이템 제거 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 장바구니 특정 아이템 제거 비즈니스 플로우 수행
 * - 사용자 권한 및 아이템 소유권 검증
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 장바구니 아이템 소유권 또는 기타 제거 규칙 검증
 * - 장바구니 상태 변경 트랜잭션 관리
 * - 제거 후 장바구니 상태 일관성 보장
 */
@Component
class RemoveCartItemUseCase(
    private val cartService: CartService
) {

    /**
     * 장바구니에서 지정된 아이템을 제거한다
     *
     * @param userId 인증된 사용자 ID
     * @param cartItemId 제거할 장바구니 아이템 ID
     * @return 아이템이 제거된 업데이트된 장바구니
     * @throws IllegalArgumentException 사용자가 소유하지 않은 아이템이거나 존재하지 않는 아이템인 경우
     */
    fun execute(userId: Long, cartItemId: Long): Cart {
        return cartService.removeCartItem(userId, cartItemId)
    }
}