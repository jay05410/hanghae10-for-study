package io.hhplus.ecommerce.cart.controller

import io.hhplus.ecommerce.cart.dto.*
import io.hhplus.ecommerce.cart.dto.CartItemResponse.Companion.toResponse
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.usecase.*
import io.hhplus.ecommerce.common.response.ApiResponse
import org.springframework.web.bind.annotation.*

/**
 * 장바구니 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 장바구니 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@RestController
@RequestMapping("/api/v1/cart")
class CartController(
    private val getCartUseCase: GetCartUseCase,
    private val addToCartUseCase: AddToCartUseCase,
    private val updateCartItemUseCase: UpdateCartItemUseCase,
    private val removeCartItemUseCase: RemoveCartItemUseCase,
    private val clearCartUseCase: ClearCartUseCase
) {

    /**
     * 사용자의 장바구니를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 장바구니 정보를 포함한 API 응답
     */
    @GetMapping
    fun getCart(@RequestParam userId: Long): ApiResponse<CartResponse?> {
        val cart = getCartUseCase.execute(userId)
        return ApiResponse.success(cart?.toResponse())
    }

    /**
     * 장바구니에 상품을 추가한다
     *
     * @param userId 사용자 ID
     * @param request 장바구니 추가 요청 데이터
     * @return 업데이트된 장바구니 정보를 포함한 API 응답
     */
    @PostMapping("/items")
    fun addToCart(
        @RequestParam userId: Long,
        @RequestBody request: AddToCartRequest
    ): ApiResponse<CartResponse> {
        val cart = addToCartUseCase.execute(userId, request)
        return ApiResponse.success(cart.toResponse())
    }

    /**
     * 장바구니 아이템의 수량을 변경한다
     *
     * @param cartItemId 변경할 장바구니 아이템 ID
     * @param userId 사용자 ID
     * @param quantity 변경할 수량
     * @return 업데이트된 장바구니 정보를 포함한 API 응답
     */
    @PutMapping("/items/{cartItemId}")
    fun updateCartItem(
        @PathVariable cartItemId: Long,
        @RequestParam userId: Long,
        @RequestParam quantity: Int
    ): ApiResponse<CartResponse> {
        val cart = updateCartItemUseCase.execute(userId, cartItemId, quantity)
        return ApiResponse.success(cart.toResponse())
    }

    /**
     * 장바구니에서 특정 아이템을 제거한다
     *
     * @param cartItemId 제거할 장바구니 아이템 ID
     * @param userId 사용자 ID
     * @return 업데이트된 장바구니 정보를 포함한 API 응답
     */
    @DeleteMapping("/items/{cartItemId}")
    fun removeCartItem(
        @PathVariable cartItemId: Long,
        @RequestParam userId: Long
    ): ApiResponse<CartResponse> {
        val cart = removeCartItemUseCase.execute(userId, cartItemId)
        return ApiResponse.success(cart.toResponse())
    }

    /**
     * 장바구니의 모든 아이템을 제거한다
     *
     * @param userId 사용자 ID
     * @return 초기화된 장바구니 정보를 포함한 API 응답
     */
    @DeleteMapping
    fun clearCart(@RequestParam userId: Long): ApiResponse<CartResponse> {
        val cart = clearCartUseCase.execute(userId)
        return ApiResponse.success(cart.toResponse())
    }
}