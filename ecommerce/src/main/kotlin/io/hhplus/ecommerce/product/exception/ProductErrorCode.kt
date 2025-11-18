package io.hhplus.ecommerce.product.exception

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
    ),

    STOCK_ALREADY_RESERVED(
        code = "PRODUCT007",
        message = "이미 예약된 재고입니다. 상품 ID: {productId}, 사용자 ID: {userId}",
        httpStatus = 409
    ),

    RESERVATION_NOT_FOUND(
        code = "PRODUCT008",
        message = "예약을 찾을 수 없습니다. 예약 ID: {reservationId}",
        httpStatus = 404
    ),

    RESERVATION_ACCESS_DENIED(
        code = "PRODUCT009",
        message = "예약에 대한 접근 권한이 없습니다. 예약 ID: {reservationId}, 사용자 ID: {userId}",
        httpStatus = 403
    ),

    RESERVATION_EXPIRED(
        code = "PRODUCT010",
        message = "예약이 만료되었습니다. 예약 ID: {reservationId}",
        httpStatus = 410
    ),

    RESERVATION_CANNOT_BE_CANCELLED(
        code = "PRODUCT011",
        message = "예약을 취소할 수 없습니다. 예약 ID: {reservationId}, 현재 상태: {currentStatus}",
        httpStatus = 409
    );
}