package com.ecommerce.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/cart")
class CartController {

    companion object {
        // 장바구니 데이터 (실제로는 사용자별로 관리되어야 함)
        private val cartItems = mutableMapOf<Int, Map<String, Any>>()
        private var nextCartItemId = 1

        init {
            // 기본 장바구니 데이터
            cartItems[1] = mapOf(
                "cartItemId" to 1,
                "product" to mapOf(
                    "id" to 1,
                    "name" to "기본 7일 박스",
                    "price" to 29000
                ),
                "combination" to mapOf(
                    "id" to 1,
                    "condition" to "피로",
                    "mood" to "활력",
                    "scent" to "시트러스"
                ),
                "quantity" to 1,
                "subtotal" to 29000,
                "isAvailable" to true,
                "stockRemaining" to 15
            )
            cartItems[2] = mapOf(
                "cartItemId" to 2,
                "product" to mapOf(
                    "id" to 1,
                    "name" to "기본 7일 박스",
                    "price" to 29000
                ),
                "combination" to mapOf(
                    "id" to 5,
                    "condition" to "스트레스",
                    "mood" to "평온",
                    "scent" to "플로럴"
                ),
                "quantity" to 1,
                "subtotal" to 29000,
                "isAvailable" to true,
                "stockRemaining" to 8
            )
            nextCartItemId = 3
        }
    }

    // 4.1 장바구니 추가
    @PostMapping
    fun addToCart(@RequestBody body: Map<String, Any>): Map<String, Any> {
        val productId = body["productId"] as? Int
        val combinationId = body["combinationId"] as? Int
        val quantity = body["quantity"] as? Int

        // 기본 검증
        if (productId == null || combinationId == null || quantity == null) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "CART001",
                    "message" to "잘못된 요청입니다"
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        }

        if (quantity != 1) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "CART003",
                    "message" to "수량은 1개만 가능합니다"
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        }

        // 장바구니 최대 개수 체크
        if (cartItems.size >= 10) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "CART001",
                    "message" to "장바구니 최대 개수를 초과했습니다"
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        }

        val cartItemId = nextCartItemId++
        return mapOf(
            "success" to true,
            "data" to mapOf(
                "cartItemId" to cartItemId,
                "message" to "장바구니에 추가되었습니다"
            ),
            "timestamp" to "2025-10-31T10:30:00Z"
        )
    }

    // 4.2 장바구니 조회
    @GetMapping
    fun getCart(): Map<String, Any> {
        val items = cartItems.values.toList()
        val totalItems = items.size
        val totalAmount = items.sumOf { (it["subtotal"] as Int) }

        return mapOf(
            "success" to true,
            "data" to mapOf(
                "items" to items,
                "summary" to mapOf(
                    "totalItems" to totalItems,
                    "totalAmount" to totalAmount
                )
            ),
            "timestamp" to "2025-10-31T10:30:00Z"
        )
    }

    // 4.3 장바구니 항목 삭제
    @DeleteMapping("/items/{cartItemId}")
    fun removeCartItem(@PathVariable cartItemId: Int): Map<String, Any> {
        return if (!cartItems.containsKey(cartItemId)) {
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "CART001",
                    "message" to "장바구니 항목을 찾을 수 없습니다",
                    "details" to mapOf("cartItemId" to cartItemId)
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        } else {
            cartItems.remove(cartItemId)
            mapOf(
                "success" to true,
                "data" to mapOf(
                    "message" to "장바구니 항목이 삭제되었습니다"
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        }
    }

    // 4.4 장바구니 비우기
    @DeleteMapping
    fun clearCart(): Map<String, Any> {
        cartItems.clear()
        return mapOf(
            "success" to true,
            "data" to mapOf(
                "message" to "장바구니가 비워졌습니다"
            ),
            "timestamp" to "2025-10-31T10:30:00Z"
        )
    }
}