package io.hhplus.ecommerce.cart.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*

// @Entity
// @Table(name = "cart_item_tea")
data class CartItemTea(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val cartItemId: Long,

    // @Column(nullable = false)
    val productId: Long,

    // @Column(nullable = false)
    val selectionOrder: Int,

    // @Column(nullable = false)
    val ratioPercent: Int
) : ActiveJpaEntity() {

    fun validateRatioPercent() {
        require(ratioPercent in 1..100) { "배합 비율은 1-100 사이여야 합니다: $ratioPercent" }
    }

    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use ratioPercent instead", ReplaceWith("ratioPercent"))
    val quantity: Int
        get() = ratioPercent

    companion object {
        fun create(
            cartItemId: Long,
            productId: Long,
            selectionOrder: Int,
            ratioPercent: Int
        ): CartItemTea {
            require(cartItemId > 0) { "장바구니 아이템 ID는 유효해야 합니다" }
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(selectionOrder in 1..3) { "선택 순서는 1-3 사이여야 합니다" }
            require(ratioPercent in 1..100) { "배합 비율은 1-100 사이여야 합니다" }

            return CartItemTea(
                cartItemId = cartItemId,
                productId = productId,
                selectionOrder = selectionOrder,
                ratioPercent = ratioPercent
            ).also { it.validateRatioPercent() }
        }
    }
}