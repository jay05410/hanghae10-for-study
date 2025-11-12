package io.hhplus.ecommerce.order.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import jakarta.persistence.*

@Entity
@Table(name = "order_item_tea")
class OrderItemTea(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val orderItemId: Long,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false, length = 100)
    val productName: String,

    @Column(nullable = false, length = 50)
    val categoryName: String,

    @Column(nullable = false)
    val selectionOrder: Int,

    @Column(nullable = false)
    val ratioPercent: Int,

    @Column(nullable = false)
    val unitPrice: Int
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
            orderItemId: Long,
            productId: Long,
            productName: String,
            categoryName: String,
            selectionOrder: Int,
            ratioPercent: Int,
            unitPrice: Int
        ): OrderItemTea {
            require(orderItemId > 0) { "주문 아이템 ID는 유효해야 합니다" }
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(productName.isNotBlank()) { "상품명은 필수입니다" }
            require(categoryName.isNotBlank()) { "카테고리명은 필수입니다" }
            require(selectionOrder in 1..3) { "선택 순서는 1-3 사이여야 합니다" }
            require(ratioPercent in 1..100) { "배합 비율은 1-100 사이여야 합니다" }
            require(unitPrice >= 0) { "단가는 0 이상이어야 합니다" }

            return OrderItemTea(
                orderItemId = orderItemId,
                productId = productId,
                productName = productName,
                categoryName = categoryName,
                selectionOrder = selectionOrder,
                ratioPercent = ratioPercent,
                unitPrice = unitPrice
            ).also { it.validateRatioPercent() }
        }
    }
}