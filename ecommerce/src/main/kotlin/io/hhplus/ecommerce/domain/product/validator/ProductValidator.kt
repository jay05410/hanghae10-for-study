package io.hhplus.ecommerce.domain.product.validator

import io.hhplus.ecommerce.common.exception.product.ProductException
import io.hhplus.ecommerce.domain.cart.entity.CartItem
import io.hhplus.ecommerce.domain.product.entity.BoxType

/**
 * 상품 검증 유틸리티
 *
 * 책임: DB 호출 없는 순수 검증 로직만 포함
 */
object ProductValidator {

    /**
     * 재고 충분 여부 검증
     *
     * @param availableStock 현재 가용 재고
     * @param requestedQuantity 요청 수량
     * @param productId 상품 ID
     * @throws ProductException.InsufficientStock 재고 부족 시
     */
    fun validateStock(availableStock: Int, requestedQuantity: Int, productId: Long) {
        if (availableStock < requestedQuantity) {
            throw ProductException.InsufficientStock(productId, availableStock, requestedQuantity)
        }
    }

    /**
     * 박스 타입과 차 개수 일치 검증
     *
     * @param boxType 박스 타입
     * @param cartItems 장바구니 아이템들
     * @throws IllegalArgumentException 차 개수가 일치하지 않을 시
     */
    fun validateTeaCount(boxType: BoxType, cartItems: List<CartItem>) {
        val totalTeaCount = cartItems.sumOf { it.quantity }
        boxType.validateTeaCount(totalTeaCount)
    }

    /**
     * 상품 활성 상태 검증
     *
     * @param isActive 활성 상태
     * @param productName 상품명 (로깅용)
     * @throws IllegalArgumentException 비활성 상품일 시
     */
    fun validateProductActive(isActive: Boolean, productName: String) {
        require(isActive) { "비활성 상품은 주문할 수 없습니다: $productName" }
    }
}