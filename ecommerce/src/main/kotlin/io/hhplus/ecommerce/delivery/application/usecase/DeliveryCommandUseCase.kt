package io.hhplus.ecommerce.delivery.application.usecase

import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.service.DeliveryDomainService
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 배송 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 모든 배송 변경 작업을 통합 관리
 * - 배송 생성, 상태 변경 기능 제공
 *
 * 책임:
 * - 트랜잭션 경계 관리
 * - 배송 생성 및 상태 변경 요청 검증 및 실행
 * - 배송 정보 무결성 보장
 * - DeliveryDomainService 협력 (도메인 로직)
 */
@Component
class DeliveryCommandUseCase(
    private val deliveryDomainService: DeliveryDomainService
) {

    /**
     * 새로운 배송 정보를 생성
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
        // 1. 중복 배송 검증
        deliveryDomainService.validateNoDuplicateDelivery(orderId)

        // 2. 배송 생성
        return deliveryDomainService.createDelivery(orderId, deliveryAddress, deliveryMemo)
    }

    /**
     * 배송 준비를 시작
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun startPreparing(id: Long): Delivery {
        val delivery = deliveryDomainService.getDelivery(id)
        return deliveryDomainService.startPreparing(delivery)
    }

    /**
     * 배송을 시작 (발송 처리)
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
        val delivery = deliveryDomainService.getDelivery(id)
        return deliveryDomainService.ship(delivery, trackingNumber, carrier)
    }

    /**
     * 배송을 완료
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun deliver(id: Long): Delivery {
        val delivery = deliveryDomainService.getDelivery(id)
        return deliveryDomainService.deliver(delivery)
    }

    /**
     * 배송을 실패 처리
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun fail(id: Long): Delivery {
        val delivery = deliveryDomainService.getDelivery(id)
        return deliveryDomainService.fail(delivery)
    }
}
