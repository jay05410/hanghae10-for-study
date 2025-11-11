package io.hhplus.ecommerce.cart.usecase

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.product.application.ProductService
import org.springframework.stereotype.Component

/**
 * 장바구니 상품 추가 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 장바구니 상품 추가 비즈니스 플로우 조율
 * - 상품 검증 및 장바구니 서비스 연계
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 상품 존재성 검증
 * - 장바구니 추가 전 비즈니스 규칙 적용
 * - 장바구니 상태 변경 트랜잭션 관리
 */
@Component
class AddToCartUseCase(
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
    fun execute(userId: Long, request: AddToCartRequest): Cart {
        // 1. 상품 존재 여부 검증
        productService.getProduct(request.packageTypeId)

        // 2. 장바구니에 상품 추가
        return cartService.addToCart(
            userId = userId,
            packageTypeId = request.packageTypeId,
            packageTypeName = request.packageTypeName,
            packageTypeDays = request.packageTypeDays,
            dailyServing = request.dailyServing,
            totalQuantity = request.totalQuantity,
            giftWrap = request.giftWrap,
            giftMessage = request.giftMessage,
            teaItems = request.teaItems
        )
    }
}