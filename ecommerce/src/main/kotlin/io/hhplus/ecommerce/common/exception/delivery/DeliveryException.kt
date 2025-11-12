package io.hhplus.ecommerce.common.exception.delivery

import io.hhplus.ecommerce.common.exception.BusinessException
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import org.slf4j.event.Level
import java.time.LocalDateTime

/**
 * 배송 관련 예외 클래스
 *
 * 배송 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 DeliveryErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class DeliveryException(
    errorCode: DeliveryErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 배송 정보를 찾을 수 없음 예외
     */
    class DeliveryNotFound(deliveryId: Long) : DeliveryException(
        errorCode = DeliveryErrorCode.DELIVERY_NOT_FOUND,
        message = DeliveryErrorCode.DELIVERY_NOT_FOUND.withParams("deliveryId" to deliveryId),
        data = mapOf("deliveryId" to deliveryId)
    )

    /**
     * 주문에 대한 배송 정보를 찾을 수 없음 예외
     */
    class DeliveryNotFoundByOrder(orderId: Long) : DeliveryException(
        errorCode = DeliveryErrorCode.DELIVERY_NOT_FOUND_BY_ORDER,
        message = DeliveryErrorCode.DELIVERY_NOT_FOUND_BY_ORDER.withParams("orderId" to orderId),
        data = mapOf("orderId" to orderId)
    )

    /**
     * 잘못된 배송 상태 전환 예외
     */
    class InvalidDeliveryStateTransition(
        currentState: DeliveryStatus,
        attemptedState: DeliveryStatus
    ) : DeliveryException(
        errorCode = DeliveryErrorCode.INVALID_DELIVERY_STATE_TRANSITION,
        message = DeliveryErrorCode.INVALID_DELIVERY_STATE_TRANSITION.withParams(
            "currentState" to currentState,
            "attemptedState" to attemptedState
        ),
        data = mapOf(
            "currentState" to currentState,
            "attemptedState" to attemptedState
        )
    )

    /**
     * 배송지 변경 불가 예외
     */
    class DeliveryAddressChangeNotAllowed(deliveryState: DeliveryStatus) : DeliveryException(
        errorCode = DeliveryErrorCode.DELIVERY_ADDRESS_CHANGE_NOT_ALLOWED,
        message = DeliveryErrorCode.DELIVERY_ADDRESS_CHANGE_NOT_ALLOWED.withParams(
            "deliveryState" to deliveryState
        ),
        data = mapOf("deliveryState" to deliveryState)
    )

    /**
     * 반품 가능 기간 초과 예외
     */
    class ReturnPeriodExpired(deliveryDate: LocalDateTime) : DeliveryException(
        errorCode = DeliveryErrorCode.RETURN_PERIOD_EXPIRED,
        message = DeliveryErrorCode.RETURN_PERIOD_EXPIRED.withParams(
            "deliveryDate" to deliveryDate
        ),
        data = mapOf("deliveryDate" to deliveryDate)
    )

    /**
     * 운송장 정보 필수 예외
     */
    class TrackingInfoRequired : DeliveryException(
        errorCode = DeliveryErrorCode.TRACKING_INFO_REQUIRED
    )
}
