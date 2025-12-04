package io.hhplus.ecommerce.cart.presentation.controller

import io.hhplus.ecommerce.cart.application.usecase.CartCommandUseCase
import io.hhplus.ecommerce.cart.application.usecase.GetCartQueryUseCase
import io.hhplus.ecommerce.cart.presentation.dto.*
import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "장바구니", description = "장바구니 관리 API")
@RestController
@RequestMapping("/api/v1/cart")
class CartController(
    private val getCartQueryUseCase: GetCartQueryUseCase,
    private val cartCommandUseCase: CartCommandUseCase
) {

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니를 조회합니다.")
    @GetMapping
    fun getCart(
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long
    ): ApiResponse<CartResponse?> {
        val cart = getCartQueryUseCase.execute(userId)
        return ApiResponse.success(cart?.toResponse())
    }

    @Operation(summary = "장바구니에 상품 추가", description = "장바구니에 상품을 추가합니다.")
    @PostMapping("/items")
    fun addToCart(
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long,
        @Parameter(description = "장바구니 추가 정보", required = true)
        @RequestBody request: AddToCartRequest
    ): ApiResponse<CartResponse> {
        val cart = cartCommandUseCase.addToCart(userId, request)
        return ApiResponse.success(cart.toResponse())
    }

    @Operation(summary = "장바구니 아이템 수량 변경", description = "장바구니 아이템의 수량을 변경합니다.")
    @PutMapping("/items/{cartItemId}")
    fun updateCartItem(
        @Parameter(description = "장바구니 아이템 ID", required = true)
        @PathVariable cartItemId: Long,
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long,
        @Parameter(description = "변경할 수량", required = true)
        @RequestParam quantity: Int
    ): ApiResponse<CartResponse> {
        val cart = cartCommandUseCase.updateCartItem(userId, cartItemId, quantity)
        return ApiResponse.success(cart.toResponse())
    }

    @Operation(summary = "장바구니 아이템 제거", description = "장바구니에서 특정 아이템을 제거합니다.")
    @DeleteMapping("/items/{cartItemId}")
    fun removeCartItem(
        @Parameter(description = "제거할 아이템 ID", required = true)
        @PathVariable cartItemId: Long,
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long
    ): ApiResponse<CartResponse> {
        val cart = cartCommandUseCase.removeCartItem(userId, cartItemId)
        return ApiResponse.success(cart.toResponse())
    }

    @Operation(summary = "장바구니 비우기", description = "장바구니의 모든 아이템을 제거합니다.")
    @DeleteMapping
    fun clearCart(
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long
    ): ApiResponse<CartResponse> {
        val cart = cartCommandUseCase.clearCart(userId)
        return ApiResponse.success(cart.toResponse())
    }
}
