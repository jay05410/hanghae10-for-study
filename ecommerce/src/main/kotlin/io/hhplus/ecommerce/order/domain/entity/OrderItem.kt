package io.hhplus.ecommerce.order.domain.entity

import java.time.LocalDateTime

/**
 * OrderItem 도메인 모델 (immutable)
 *
 * 역할:
 * - 주문 아이템의 정보 보관
 * - JPA 의존성 제거로 도메인 순수성 유지
 */
data class OrderItem(
    val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val giftWrapPrice: Int = 0,
    val totalPrice: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val deletedAt: LocalDateTime? = null
) {
    fun validatePrices() {
        require(unitPrice >= 0) { "단가는 0 이상이어야 합니다: $unitPrice" }
        require(giftWrapPrice >= 0) { "선물포장 가격은 0 이상이어야 합니다: $giftWrapPrice" }
        require(totalPrice == (unitPrice + giftWrapPrice) * quantity) {
            "총 가격이 올바르지 않습니다: 계산값=${(unitPrice + giftWrapPrice) * quantity}, 실제값=$totalPrice"
        }
    }

    companion object {
        fun create(
            orderId: Long,
            productId: Long,
            productName: String,
            categoryName: String,
            quantity: Int,
            unitPrice: Int,
            giftWrap: Boolean = false,
            giftMessage: String? = null,
            giftWrapPrice: Int = 0
        ): OrderItem {
            require(orderId >= 0) { "주문 ID는 유효해야 합니다" }
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(productName.isNotBlank()) { "상품명은 필수입니다" }
            require(categoryName.isNotBlank()) { "카테고리명은 필수입니다" }
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(unitPrice >= 0) { "단가는 0 이상이어야 합니다" }
            require(giftWrapPrice >= 0) { "선물포장 가격은 0 이상이어야 합니다" }

            val totalPrice = (unitPrice + giftWrapPrice) * quantity

            return OrderItem(
                orderId = orderId,
                productId = productId,
                productName = productName,
                categoryName = categoryName,
                quantity = quantity,
                unitPrice = unitPrice,
                giftWrap = giftWrap,
                giftMessage = giftMessage,
                giftWrapPrice = giftWrapPrice,
                totalPrice = totalPrice
            ).also { it.validatePrices() }
        }
    }
}