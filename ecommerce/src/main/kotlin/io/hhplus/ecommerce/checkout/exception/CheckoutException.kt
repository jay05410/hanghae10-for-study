package io.hhplus.ecommerce.checkout.exception

import io.hhplus.ecommerce.common.exception.BusinessException
import io.hhplus.ecommerce.common.exception.ErrorCode
import org.slf4j.event.Level

/**
 * 체크아웃 예외
 */
sealed class CheckoutException(
    errorCode: CheckoutErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap()
) : BusinessException(errorCode, message, logLevel, data) {

    class InsufficientStock(productId: Long, availableQuantity: Int, requestedQuantity: Int) : CheckoutException(
        errorCode = CheckoutErrorCode.INSUFFICIENT_STOCK,
        message = CheckoutErrorCode.INSUFFICIENT_STOCK.withParams(
            "productId" to productId,
            "availableQuantity" to availableQuantity,
            "requestedQuantity" to requestedQuantity
        ),
        data = mapOf(
            "productId" to productId,
            "availableQuantity" to availableQuantity,
            "requestedQuantity" to requestedQuantity
        )
    )

    class CheckoutExpired(orderId: Long) : CheckoutException(
        errorCode = CheckoutErrorCode.CHECKOUT_EXPIRED,
        message = CheckoutErrorCode.CHECKOUT_EXPIRED.withParams("orderId" to orderId),
        data = mapOf("orderId" to orderId)
    )

    class CheckoutNotFound(orderId: Long) : CheckoutException(
        errorCode = CheckoutErrorCode.CHECKOUT_NOT_FOUND,
        message = CheckoutErrorCode.CHECKOUT_NOT_FOUND.withParams("orderId" to orderId),
        data = mapOf("orderId" to orderId)
    )

    class InvalidCheckoutState(orderId: Long, currentState: String) : CheckoutException(
        errorCode = CheckoutErrorCode.INVALID_CHECKOUT_STATE,
        message = CheckoutErrorCode.INVALID_CHECKOUT_STATE.withParams(
            "orderId" to orderId,
            "currentState" to currentState
        ),
        data = mapOf("orderId" to orderId, "currentState" to currentState)
    )

    class ProductNotFound(productId: Long) : CheckoutException(
        errorCode = CheckoutErrorCode.PRODUCT_NOT_FOUND,
        message = CheckoutErrorCode.PRODUCT_NOT_FOUND.withParams("productId" to productId),
        data = mapOf("productId" to productId)
    )

    class CartItemsNotFound(cartItemIds: List<Long>) : CheckoutException(
        errorCode = CheckoutErrorCode.CART_ITEMS_NOT_FOUND,
        message = CheckoutErrorCode.CART_ITEMS_NOT_FOUND.withParams("cartItemIds" to cartItemIds),
        data = mapOf("cartItemIds" to cartItemIds)
    )

    class InsufficientBalance(userId: Long, requiredAmount: Long, currentBalance: Long) : CheckoutException(
        errorCode = CheckoutErrorCode.INSUFFICIENT_BALANCE,
        message = CheckoutErrorCode.INSUFFICIENT_BALANCE.withParams(
            "requiredAmount" to requiredAmount,
            "currentBalance" to currentBalance
        ),
        data = mapOf(
            "userId" to userId,
            "requiredAmount" to requiredAmount,
            "currentBalance" to currentBalance
        )
    )
}

enum class CheckoutErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null
) : ErrorCode {
    INSUFFICIENT_STOCK(
        code = "CHECKOUT_001",
        message = "재고가 부족합니다. 상품 ID: {productId}, 가용 재고: {availableQuantity}, 요청 수량: {requestedQuantity}",
        httpStatus = 409
    ),
    CHECKOUT_EXPIRED(
        code = "CHECKOUT_002",
        message = "체크아웃 세션이 만료되었습니다. 주문 ID: {orderId}",
        httpStatus = 410
    ),
    CHECKOUT_NOT_FOUND(
        code = "CHECKOUT_003",
        message = "체크아웃 정보를 찾을 수 없습니다. 주문 ID: {orderId}",
        httpStatus = 404
    ),
    INVALID_CHECKOUT_STATE(
        code = "CHECKOUT_004",
        message = "체크아웃 상태가 올바르지 않습니다. 주문 ID: {orderId}, 현재 상태: {currentState}",
        httpStatus = 400
    ),
    PRODUCT_NOT_FOUND(
        code = "CHECKOUT_005",
        message = "상품을 찾을 수 없습니다. 상품 ID: {productId}",
        httpStatus = 404
    ),
    CART_ITEMS_NOT_FOUND(
        code = "CHECKOUT_006",
        message = "장바구니 아이템을 찾을 수 없습니다. 아이템 ID: {cartItemIds}",
        httpStatus = 404
    ),
    INSUFFICIENT_BALANCE(
        code = "CHECKOUT_007",
        message = "포인트 잔액이 부족합니다. 필요 금액: {requiredAmount}원, 현재 잔액: {currentBalance}원",
        httpStatus = 402
    )
}
