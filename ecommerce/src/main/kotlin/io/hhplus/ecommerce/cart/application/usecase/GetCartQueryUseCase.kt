package io.hhplus.ecommerce.cart.application.usecase

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 조회 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 장바구니 정보 조회
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 읽기 전용 작업 처리
 * - 사용자 권한 기반 장바구니 조회
 */
@Component
class GetCartQueryUseCase(
    private val cartDomainService: CartDomainService
) {

    /**
     * 사용자의 현재 장바구니 조회
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 장바구니 (없으면 null 반환)
     */
    @Transactional(readOnly = true)
    fun execute(userId: Long): Cart? {
        return cartDomainService.getCartByUser(userId)
    }
}
