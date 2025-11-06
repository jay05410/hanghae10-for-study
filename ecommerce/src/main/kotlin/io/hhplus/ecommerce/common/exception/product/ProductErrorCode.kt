package io.hhplus.ecommerce.common.exception.product

import io.hhplus.ecommerce.common.exception.ErrorCode

/**
 * 상품 관련 에러 코드
 */
enum class ProductErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    PRODUCT_NOT_FOUND(
        code = "PRODUCT001",
        message = "상품을 찾을 수 없습니다. ID: {productId}",
        httpStatus = 404
    ),

    CATEGORY_NOT_FOUND(
        code = "PRODUCT002",
        message = "카테고리를 찾을 수 없습니다. ID: {categoryId}",
        httpStatus = 404
    ),

    BOX_TYPE_NOT_FOUND(
        code = "PRODUCT003",
        message = "박스 타입을 찾을 수 없습니다. ID: {boxTypeId}",
        httpStatus = 404
    ),

    INSUFFICIENT_STOCK(
        code = "PRODUCT004",
        message = "재고가 부족합니다. 상품 ID: {productId}, 가용재고: {availableStock}, 요청수량: {requestedQuantity}",
        httpStatus = 409
    ),

    DAILY_PRODUCTION_LIMIT_EXCEEDED(
        code = "PRODUCT005",
        message = "일일 생산 한도를 초과했습니다. 박스타입: {boxTypeName}, 한도: {dailyLimit}, 오늘주문: {todayOrdered}",
        httpStatus = 409
    ),

    INVENTORY_NOT_FOUND(
        code = "PRODUCT006",
        message = "재고 정보를 찾을 수 없습니다. 상품 ID: {productId}",
        httpStatus = 404
    );
}