package io.hhplus.ecommerce.delivery.usecase

import io.hhplus.ecommerce.delivery.application.DeliveryService
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 배송 명령 UseCase
 *
 * 역할:
 * - 모든 배송 변경 작업을 통합 관리
 * - 배송 생성, 상태 변경 기능 제공
 *
 * 책임:
 * - 배송 생성 및 상태 변경 요청 검증 및 실행
 * - 배송 정보 무결성 보장
 */
@Component
class DeliveryCommandUseCase(
    private val deliveryService: DeliveryService
) {

    /**
     * 새로운 배송 정보를 생성합니다.
     *
     * @param orderId 주문 ID
     * @param deliveryAddress 배송지 정보
     * @param deliveryMemo 배송 메모
     * @return 생성된 배송 정보
     * @throws IllegalStateException 이미 배송 정보가 존재하는 경우
     */
    @Transactional
    fun createDelivery(
        orderId: Long,
        deliveryAddress: DeliveryAddress,
        deliveryMemo: String? = null
    ): Delivery {
        return deliveryService.createDelivery(orderId, deliveryAddress, deliveryMemo)
    }

    /**
     * 배송 준비를 시작합니다.
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun startPreparing(id: Long): Delivery {
        return deliveryService.startPreparing(id)
    }

    /**
     * 배송을 시작합니다.
     *
     * @param id 배송 ID
     * @param trackingNumber 운송장 번호
     * @param carrier 택배사
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun ship(
        id: Long,
        trackingNumber: String,
        carrier: String
    ): Delivery {
        return deliveryService.ship(id, trackingNumber, carrier)
    }

    /**
     * 배송을 완료합니다.
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun deliver(id: Long): Delivery {
        return deliveryService.deliver(id)
    }

    /**
     * 배송을 실패 처리합니다.
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun fail(id: Long): Delivery {
        return deliveryService.fail(id)
    }
}