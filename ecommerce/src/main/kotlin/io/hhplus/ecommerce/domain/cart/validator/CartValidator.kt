package io.hhplus.ecommerce.domain.cart.validator

import io.hhplus.ecommerce.common.exception.cart.CartException
import io.hhplus.ecommerce.domain.cart.entity.CartItem

/**
 * 장바구니 검증 유틸리티
 *
 * 책임: DB 호출 없는 순수 검증 로직만 포함
 */
object CartValidator {

    private const val MAX_CART_ITEMS = 3

    /**
     * 장바구니 최대 아이템 수 검증
     *
     * @param currentItemCount 현재 장바구니 아이템 수
     * @throws CartException.MaxItemsExceeded 최대 아이템 수 초과 시
     */
    fun validateMaxItems(currentItemCount: Int) {
        if (currentItemCount >= MAX_CART_ITEMS) {
            throw CartException.MaxItemsExceeded(currentItemCount, MAX_CART_ITEMS)
        }
    }

    /**
     * 중복 박스 타입 검증
     *
     * @param items 현재 장바구니 아이템들
     * @param newBoxTypeId 추가하려는 박스 타입 ID
     * @throws CartException.DuplicateBoxType 중복 박스 타입 시
     */
    fun validateDuplicateBoxType(items: List<CartItem>, newBoxTypeId: Long) {
        val hasBoxType = items.any { it.boxTypeId == newBoxTypeId }
        if (hasBoxType) {
            throw CartException.DuplicateBoxType(newBoxTypeId)
        }
    }

    /**
     * 빈 장바구니 검증
     *
     * @param items 장바구니 아이템들
     * @throws CartException.EmptyCart 장바구니가 비어있을 시
     */
    fun validateNotEmpty(items: List<CartItem>) {
        if (items.isEmpty()) {
            throw CartException.EmptyCart()
        }
    }
}