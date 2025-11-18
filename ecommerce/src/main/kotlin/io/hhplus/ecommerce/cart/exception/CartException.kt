package io.hhplus.ecommerce.cart.exception

import io.hhplus.ecommerce.common.exception.BusinessException
import org.slf4j.event.Level

/**
 * 장바구니 관련 예외 클래스
 *
 * 장바구니 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 CartErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class CartException(
    errorCode: CartErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 장바구니 최대 아이템 수 초과 예외
     */
    class MaxItemsExceeded(currentCount: Int, maxItems: Int) : CartException(
        errorCode = CartErrorCode.MAX_ITEMS_EXCEEDED,
        message = CartErrorCode.MAX_ITEMS_EXCEEDED.withParams(
            "currentCount" to currentCount,
            "maxItems" to maxItems
        ),
        data = mapOf(
            "currentCount" to currentCount,
            "maxItems" to maxItems
        )
    )

    /**
     * 중복 박스 타입 예외
     */
    class DuplicateBoxType(boxTypeId: Long) : CartException(
        errorCode = CartErrorCode.DUPLICATE_BOX_TYPE,
        message = CartErrorCode.DUPLICATE_BOX_TYPE.withParams("boxTypeId" to boxTypeId),
        data = mapOf("boxTypeId" to boxTypeId)
    )

    /**
     * 차 개수 불일치 예외
     */
    class TeaCountMismatch(boxDays: Int, selectedCount: Int) : CartException(
        errorCode = CartErrorCode.TEA_COUNT_MISMATCH,
        message = CartErrorCode.TEA_COUNT_MISMATCH.withParams(
            "boxDays" to boxDays,
            "selectedCount" to selectedCount
        ),
        data = mapOf(
            "boxDays" to boxDays,
            "selectedCount" to selectedCount
        )
    )

    /**
     * 차 비율 오류 예외
     */
    class InvalidTeaRatio(reason: String) : CartException(
        errorCode = CartErrorCode.INVALID_TEA_RATIO,
        message = reason,
        data = mapOf("reason" to reason)
    )

    /**
     * 빈 장바구니 예외
     */
    class EmptyCart : CartException(
        errorCode = CartErrorCode.EMPTY_CART
    )

    /**
     * 장바구니 아이템을 찾을 수 없음 예외
     */
    class CartItemNotFound(cartItemId: Long) : CartException(
        errorCode = CartErrorCode.CART_ITEM_NOT_FOUND,
        message = CartErrorCode.CART_ITEM_NOT_FOUND.withParams("cartItemId" to cartItemId),
        data = mapOf("cartItemId" to cartItemId)
    )

    /**
     * 장바구니를 찾을 수 없음 예외
     */
    class CartNotFound(userId: Long) : CartException(
        errorCode = CartErrorCode.CART_NOT_FOUND,
        message = CartErrorCode.CART_NOT_FOUND.withParams("userId" to userId),
        data = mapOf("userId" to userId)
    )
}