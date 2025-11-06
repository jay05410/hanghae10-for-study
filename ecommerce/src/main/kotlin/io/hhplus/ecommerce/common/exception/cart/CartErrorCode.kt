package io.hhplus.ecommerce.common.exception.cart

import io.hhplus.ecommerce.common.exception.ErrorCode

/**
 * 장바구니 관련 에러 코드
 */
enum class CartErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    MAX_ITEMS_EXCEEDED(
        code = "CART001",
        message = "장바구니는 최대 {maxItems}개까지 담을 수 있습니다. 현재 항목: {currentCount}",
        httpStatus = 409
    ),

    DUPLICATE_BOX_TYPE(
        code = "CART002",
        message = "동일한 박스타입이 이미 장바구니에 있습니다. 박스타입 ID: {boxTypeId}",
        httpStatus = 409
    ),

    TEA_COUNT_MISMATCH(
        code = "CART003",
        message = "차 선택 개수가 올바르지 않습니다. 박스일수: {boxDays}, 선택개수: {selectedCount}",
        httpStatus = 400
    ),

    INVALID_TEA_RATIO(
        code = "CART004",
        message = "차 선택 비율이 올바르지 않습니다",
        httpStatus = 400
    ),

    EMPTY_CART(
        code = "CART005",
        message = "장바구니가 비어있습니다. 상품을 추가한 후 주문해주세요",
        httpStatus = 400
    ),

    CART_ITEM_NOT_FOUND(
        code = "CART006",
        message = "장바구니 아이템을 찾을 수 없습니다. ID: {cartItemId}",
        httpStatus = 404
    ),

    CART_NOT_FOUND(
        code = "CART007",
        message = "장바구니를 찾을 수 없습니다. 사용자 ID: {userId}",
        httpStatus = 404
    );
}