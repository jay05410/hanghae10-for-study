package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import org.springframework.stereotype.Component

/**
 * 장바구니 조회 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 장바구니 정보 조회
 * - 장바구니 데이터 반환 업무 처리
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 사용자 권한 기반 장바구니 조회
 * - 장바구니 데이터 변환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetCartUseCase(
    private val cartService: CartService
) {

    /**
     * 사용자의 현재 장바구니를 조회한다
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 장바구니 (없으면 null 반환)
     */
    fun execute(userId: Long): Cart? {
        return cartService.getCartByUser(userId)
    }
}