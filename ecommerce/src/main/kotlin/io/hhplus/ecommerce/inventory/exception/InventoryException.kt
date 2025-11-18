package io.hhplus.ecommerce.inventory.exception

import io.hhplus.ecommerce.common.exception.BusinessException
import org.slf4j.event.Level

sealed class InventoryException(
    errorCode: InventoryErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    class InventoryNotFound(productId: Long) : InventoryException(
        errorCode = InventoryErrorCode.INVENTORY_NOT_FOUND,
        message = InventoryErrorCode.INVENTORY_NOT_FOUND.withParams("productId" to productId),
        data = mapOf("productId" to productId)
    )

    class InventoryAlreadyExists(productId: Long) : InventoryException(
        errorCode = InventoryErrorCode.INVENTORY_ALREADY_EXISTS,
        message = InventoryErrorCode.INVENTORY_ALREADY_EXISTS.withParams("productId" to productId),
        data = mapOf("productId" to productId)
    )

    class InsufficientStock(productId: Long, availableStock: Int, requestedQuantity: Int) : InventoryException(
        errorCode = InventoryErrorCode.INSUFFICIENT_STOCK,
        message = InventoryErrorCode.INSUFFICIENT_STOCK.withParams(
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

    class StockAlreadyReserved(productId: Long, userId: Long) : InventoryException(
        errorCode = InventoryErrorCode.STOCK_ALREADY_RESERVED,
        message = InventoryErrorCode.STOCK_ALREADY_RESERVED.withParams("productId" to productId, "userId" to userId),
        data = mapOf("productId" to productId, "userId" to userId)
    )

    class ReservationNotFound(reservationId: Long) : InventoryException(
        errorCode = InventoryErrorCode.RESERVATION_NOT_FOUND,
        message = InventoryErrorCode.RESERVATION_NOT_FOUND.withParams("reservationId" to reservationId),
        data = mapOf("reservationId" to reservationId)
    )

    class ReservationAccessDenied(reservationId: Long, userId: Long) : InventoryException(
        errorCode = InventoryErrorCode.RESERVATION_ACCESS_DENIED,
        message = InventoryErrorCode.RESERVATION_ACCESS_DENIED.withParams("reservationId" to reservationId, "userId" to userId),
        data = mapOf("reservationId" to reservationId, "userId" to userId)
    )

    class ReservationExpired(reservationId: Long) : InventoryException(
        errorCode = InventoryErrorCode.RESERVATION_EXPIRED,
        message = InventoryErrorCode.RESERVATION_EXPIRED.withParams("reservationId" to reservationId),
        data = mapOf("reservationId" to reservationId)
    )

    class ReservationCannotBeCancelled(reservationId: Long, currentStatus: Any) : InventoryException(
        errorCode = InventoryErrorCode.RESERVATION_CANNOT_BE_CANCELLED,
        message = InventoryErrorCode.RESERVATION_CANNOT_BE_CANCELLED.withParams("reservationId" to reservationId, "currentStatus" to currentStatus),
        data = mapOf("reservationId" to reservationId, "currentStatus" to currentStatus)
    )
}