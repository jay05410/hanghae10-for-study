package io.hhplus.ecommerce.delivery.usecase

import io.hhplus.ecommerce.delivery.application.DeliveryService
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import org.springframework.stereotype.Component

/**
 * 배송 정보 조회 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 배송 정보 조회 비즈니스 플로우 조율
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 단일 배송 정보 조회
 * - 주문별 배송 정보 조회
 * - 운송장 번호 기반 배송 조회
 */
@Component
class GetDeliveryQueryUseCase(
    private val deliveryService: DeliveryService
) {

    /**
     * 배송 ID로 배송 정보를 조회
     *
     * @param deliveryId 조회할 배송 ID
     * @return 배송 엔티티
     * @throws io.hhplus.ecommerce.common.exception.delivery.DeliveryException.DeliveryNotFound 배송을 찾을 수 없을 때
     */
    fun getDelivery(deliveryId: Long): Delivery {
        return deliveryService.getDelivery(deliveryId)
    }

    /**
     * 주문 ID로 배송 정보를 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 배송 엔티티
     * @throws io.hhplus.ecommerce.common.exception.delivery.DeliveryException.DeliveryNotFoundByOrder 배송을 찾을 수 없을 때
     */
    fun getDeliveryByOrderId(orderId: Long): Delivery {
        return deliveryService.getDeliveryByOrderId(orderId)
    }

    /**
     * 운송장 번호로 배송 정보를 조회
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 엔티티
     * @throws io.hhplus.ecommerce.common.exception.delivery.DeliveryException.DeliveryNotFound 배송을 찾을 수 없을 때
     */
    fun getDeliveryByTrackingNumber(trackingNumber: String): Delivery {
        return deliveryService.getDeliveryByTrackingNumber(trackingNumber)
    }

    /**
     * 여러 주문의 배송 정보를 조회
     *
     * @param orderIds 조회할 주문 ID 목록
     * @return 배송 엔티티 목록
     */
    fun getDeliveriesByOrderIds(orderIds: List<Long>): List<Delivery> {
        return deliveryService.getDeliveriesByOrderIds(orderIds)
    }
}
