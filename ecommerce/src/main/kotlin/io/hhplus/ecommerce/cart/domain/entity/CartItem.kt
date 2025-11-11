package io.hhplus.ecommerce.cart.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*
import java.time.LocalDateTime

// @Entity
// @Table(name = "cart_item")
class CartItem(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val cartId: Long,

    // @Column(nullable = false)
    val packageTypeId: Long,

    // @Column(nullable = false, length = 100)
    val packageTypeName: String,

    // @Column(nullable = false)
    val packageTypeDays: Int,

    // @Column(nullable = false)
    val dailyServing: Int = 1,

    // @Column(nullable = false)
    val totalQuantity: Double,

    // @Column(nullable = false)
    val giftWrap: Boolean = false,

    // @Column(length = 500)
    val giftMessage: String? = null
) : ActiveJpaEntity() {
    fun validateTotalQuantity() {
        require(totalQuantity > 0) { "총 그램수는 0보다 커야 합니다: $totalQuantity" }
    }

    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use packageTypeId instead", ReplaceWith("packageTypeId"))
    val boxTypeId: Long
        get() = packageTypeId

    @Deprecated("Use totalQuantity instead", ReplaceWith("totalQuantity.toInt()"))
    val quantity: Int
        get() = totalQuantity.toInt()

    companion object {
        fun create(
            cartId: Long,
            packageTypeId: Long,
            packageTypeName: String,
            packageTypeDays: Int,
            dailyServing: Int,
            totalQuantity: Double,
            giftWrap: Boolean = false,
            giftMessage: String? = null
        ): CartItem {
            require(cartId > 0) { "장바구니 ID는 유효해야 합니다" }
            require(packageTypeId > 0) { "박스 타입 ID는 유효해야 합니다" }
            require(packageTypeName.isNotBlank()) { "박스 타입명은 필수입니다" }
            require(packageTypeDays > 0) { "일수는 0보다 커야 합니다" }
            require(dailyServing in 1..3) { "하루 섭취량은 1-3 사이여야 합니다" }
            require(totalQuantity > 0) { "총 그램수는 0보다 커야 합니다" }

            return CartItem(
                cartId = cartId,
                packageTypeId = packageTypeId,
                packageTypeName = packageTypeName,
                packageTypeDays = packageTypeDays,
                dailyServing = dailyServing,
                totalQuantity = totalQuantity,
                giftWrap = giftWrap,
                giftMessage = giftMessage
            ).also { it.validateTotalQuantity() }
        }
    }
}