package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import org.springframework.stereotype.Component

/**
 * 장바구니 비우기 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 장바구니 전체 비우기 수행
 * - 장바구니 초기화 비즈니스 플로우 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 장바구니 비우기 사전 및 사후 처리
 * - 사용자 권한 검증
 * - 장바구니 상태 변경 트랜잭션 관리
 */
@Component
class ClearCartUseCase(
    private val cartService: CartService
) {

    /**
     * 사용자의 장바구니를 완전히 비운다
     *
     * @param userId 인증된 사용자 ID
     * @return 모든 아이템이 제거된 비어있는 장바구니
     */
    fun execute(userId: Long): Cart {
        return cartService.clearCart(userId)
    }
}