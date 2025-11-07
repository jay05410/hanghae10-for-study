package io.hhplus.ecommerce.common.exception.inventory

import io.hhplus.ecommerce.common.exception.ErrorCode

enum class InventoryErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    INVENTORY_NOT_FOUND(
        code = "INVENTORY001",
        message = "재고 정보를 찾을 수 없습니다. 상품 ID: {productId}",
        httpStatus = 404
    ),

    INVENTORY_ALREADY_EXISTS(
        code = "INVENTORY002",
        message = "이미 재고 정보가 존재합니다. 상품 ID: {productId}",
        httpStatus = 409
    ),

    INSUFFICIENT_STOCK(
        code = "INVENTORY003",
        message = "재고가 부족합니다. 상품 ID: {productId}, 가용재고: {availableStock}, 요청수량: {requestedQuantity}",
        httpStatus = 409
    ),

    STOCK_ALREADY_RESERVED(
        code = "INVENTORY004",
        message = "이미 예약된 재고입니다. 상품 ID: {productId}, 사용자 ID: {userId}",
        httpStatus = 409
    ),

    RESERVATION_NOT_FOUND(
        code = "INVENTORY005",
        message = "예약을 찾을 수 없습니다. 예약 ID: {reservationId}",
        httpStatus = 404
    ),

    RESERVATION_ACCESS_DENIED(
        code = "INVENTORY006",
        message = "예약에 대한 접근 권한이 없습니다. 예약 ID: {reservationId}, 사용자 ID: {userId}",
        httpStatus = 403
    ),

    RESERVATION_EXPIRED(
        code = "INVENTORY007",
        message = "예약이 만료되었습니다. 예약 ID: {reservationId}",
        httpStatus = 410
    ),

    RESERVATION_CANNOT_BE_CANCELLED(
        code = "INVENTORY008",
        message = "예약을 취소할 수 없습니다. 예약 ID: {reservationId}, 현재 상태: {currentStatus}",
        httpStatus = 409
    );
}