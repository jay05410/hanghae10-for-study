package io.hhplus.ecommerce.common.exception.product

import io.hhplus.ecommerce.common.exception.product.ProductErrorCode
import io.hhplus.ecommerce.common.exception.BusinessException
import org.slf4j.event.Level

/**
 * 상품 관련 예외 클래스
 *
 * 상품 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 ProductErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class ProductException(
    errorCode: ProductErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 상품을 찾을 수 없음 예외
     */
    class ProductNotFound(productId: Long) : ProductException(
        errorCode = ProductErrorCode.PRODUCT_NOT_FOUND,
        message = ProductErrorCode.PRODUCT_NOT_FOUND.withParams("productId" to productId),
        data = mapOf("productId" to productId)
    )

    /**
     * 카테고리를 찾을 수 없음 예외
     */
    class CategoryNotFound(categoryId: Long) : ProductException(
        errorCode = ProductErrorCode.CATEGORY_NOT_FOUND,
        message = ProductErrorCode.CATEGORY_NOT_FOUND.withParams("categoryId" to categoryId),
        data = mapOf("categoryId" to categoryId)
    )

    /**
     * 박스 타입을 찾을 수 없음 예외
     */
    class BoxTypeNotFound(boxTypeId: Long) : ProductException(
        errorCode = ProductErrorCode.BOX_TYPE_NOT_FOUND,
        message = ProductErrorCode.BOX_TYPE_NOT_FOUND.withParams("boxTypeId" to boxTypeId),
        data = mapOf("boxTypeId" to boxTypeId)
    )

    /**
     * 재고 부족 예외
     */
    class InsufficientStock(productId: Long, availableStock: Int, requestedQuantity: Int) : ProductException(
        errorCode = ProductErrorCode.INSUFFICIENT_STOCK,
        message = ProductErrorCode.INSUFFICIENT_STOCK.withParams(
            "productId" to productId,
            "availableStock" to availableStock,
            "requestedQuantity" to requestedQuantity
        ),
        data = mapOf(
            "productId" to productId,
            "availableStock" to availableStock,
            "requestedQuantity" to requestedQuantity
        )
    )

    /**
     * 일일 생산 한도 초과 예외
     */
    class DailyProductionLimitExceeded(boxTypeName: String, dailyLimit: Int, todayOrdered: Int) : ProductException(
        errorCode = ProductErrorCode.DAILY_PRODUCTION_LIMIT_EXCEEDED,
        message = ProductErrorCode.DAILY_PRODUCTION_LIMIT_EXCEEDED.withParams(
            "boxTypeName" to boxTypeName,
            "dailyLimit" to dailyLimit,
            "todayOrdered" to todayOrdered
        ),
        data = mapOf(
            "boxTypeName" to boxTypeName,
            "dailyLimit" to dailyLimit,
            "todayOrdered" to todayOrdered
        )
    )

    /**
     * 재고 정보를 찾을 수 없음 예외
     */
    class InventoryNotFound(productId: Long) : ProductException(
        errorCode = ProductErrorCode.INVENTORY_NOT_FOUND,
        message = ProductErrorCode.INVENTORY_NOT_FOUND.withParams("productId" to productId),
        data = mapOf("productId" to productId)
    )

    /**
     * 이미 예약된 재고 예외
     */
    class StockAlreadyReserved(productId: Long, userId: Long) : ProductException(
        errorCode = ProductErrorCode.STOCK_ALREADY_RESERVED,
        message = ProductErrorCode.STOCK_ALREADY_RESERVED.withParams("productId" to productId, "userId" to userId),
        data = mapOf("productId" to productId, "userId" to userId)
    )

    /**
     * 예약을 찾을 수 없음 예외
     */
    class ReservationNotFound(reservationId: Long) : ProductException(
        errorCode = ProductErrorCode.RESERVATION_NOT_FOUND,
        message = ProductErrorCode.RESERVATION_NOT_FOUND.withParams("reservationId" to reservationId),
        data = mapOf("reservationId" to reservationId)
    )

    /**
     * 예약 접근 권한 없음 예외
     */
    class ReservationAccessDenied(reservationId: Long, userId: Long) : ProductException(
        errorCode = ProductErrorCode.RESERVATION_ACCESS_DENIED,
        message = ProductErrorCode.RESERVATION_ACCESS_DENIED.withParams("reservationId" to reservationId, "userId" to userId),
        data = mapOf("reservationId" to reservationId, "userId" to userId)
    )

    /**
     * 예약 만료 예외
     */
    class ReservationExpired(reservationId: Long) : ProductException(
        errorCode = ProductErrorCode.RESERVATION_EXPIRED,
        message = ProductErrorCode.RESERVATION_EXPIRED.withParams("reservationId" to reservationId),
        data = mapOf("reservationId" to reservationId)
    )

    /**
     * 예약 취소 불가 예외
     */
    class ReservationCannotBeCancelled(reservationId: Long, currentStatus: Any) : ProductException(
        errorCode = ProductErrorCode.RESERVATION_CANNOT_BE_CANCELLED,
        message = ProductErrorCode.RESERVATION_CANNOT_BE_CANCELLED.withParams("reservationId" to reservationId, "currentStatus" to currentStatus),
        data = mapOf("reservationId" to reservationId, "currentStatus" to currentStatus)
    )
}