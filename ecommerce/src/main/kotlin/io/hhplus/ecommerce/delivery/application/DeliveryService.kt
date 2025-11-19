package io.hhplus.ecommerce.delivery.application

import io.hhplus.ecommerce.delivery.exception.DeliveryException
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.repository.DeliveryRepository
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 배송 도메인 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 배송 도메인의 핵심 비즈니스 로직 처리
 * - 배송 생명주기 관리 및 상태 변경
 * - 배송 정보 조회 및 관리
 *
 * 책임:
 * - 배송 생성, 수정, 조회
 * - 배송 상태 변경 (발송, 배송 완료 등)
 * - 배송지 정보 스냅샷 관리
 */
@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository
) {

    /**
     * 새로운 배송을 생성
     *
     * @param orderId 주문 ID
     * @param deliveryAddress 배송지 정보
     * @param deliveryMemo 배송 메모
     * @return 생성된 배송 엔티티
     */
    @Transactional
    fun createDelivery(
        orderId: Long,
        deliveryAddress: DeliveryAddress,
        deliveryMemo: String?
    ): Delivery {
        // 주문에 대한 배송이 이미 존재하는지 확인 (1:1 관계)
        deliveryRepository.findByOrderId(orderId)?.let {
            throw IllegalStateException("주문 ID $orderId 에 대한 배송 정보가 이미 존재합니다")
        }

        val delivery = Delivery.create(
            orderId = orderId,
            deliveryAddress = deliveryAddress,
            deliveryMemo = deliveryMemo
        )

        return deliveryRepository.save(delivery)
    }

    /**
     * 배송 ID로 배송 정보 조회
     *
     * @param id 배송 ID
     * @return 배송 엔티티
     * @throws DeliveryException.DeliveryNotFound 배송을 찾을 수 없을 때
     */
    @Transactional(readOnly = true)
    fun getDelivery(id: Long): Delivery {
        return deliveryRepository.findById(id)
            ?: throw DeliveryException.DeliveryNotFound(id)
    }

    /**
     * 주문 ID로 배송 정보 조회
     *
     * @param orderId 주문 ID
     * @return 배송 엔티티
     * @throws DeliveryException.DeliveryNotFoundByOrder 배송을 찾을 수 없을 때
     */
    @Transactional(readOnly = true)
    fun getDeliveryByOrderId(orderId: Long): Delivery {
        return deliveryRepository.findByOrderId(orderId)
            ?: throw DeliveryException.DeliveryNotFoundByOrder(orderId)
    }

    /**
     * 여러 주문 ID로 배송 정보 목록 조회
     *
     * @param orderIds 주문 ID 목록
     * @return 배송 엔티티 목록
     */
    @Transactional(readOnly = true)
    fun getDeliveriesByOrderIds(orderIds: List<Long>): List<Delivery> {
        return deliveryRepository.findByOrderIdIn(orderIds)
    }

    /**
     * 배송 준비 시작
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 엔티티
     */
    @Transactional
    fun startPreparing(id: Long): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.startPreparing()
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 발송 처리 (운송장 등록)
     *
     * @param id 배송 ID
     * @param trackingNumber 운송장 번호
     * @param carrier 택배사
     * @return 업데이트된 배송 엔티티
     */
    @Transactional
    fun ship(
        id: Long,
        trackingNumber: String,
        carrier: String
    ): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.ship(trackingNumber, carrier)
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 완료 처리
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 엔티티
     */
    @Transactional
    fun deliver(id: Long): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.deliver()
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 실패 처리
     *
     * @param id 배송 ID
     * @return 업데이트된 배송 엔티티
     */
    @Transactional
    fun fail(id: Long): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.fail()
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 상태로 배송 목록 조회
     *
     * @param status 배송 상태
     * @return 배송 엔티티 목록
     */
    @Transactional(readOnly = true)
    fun getDeliveriesByStatus(status: DeliveryStatus): List<Delivery> {
        return deliveryRepository.findByStatus(status)
    }

    /**
     * 운송장 번호로 배송 조회
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 엔티티
     * @throws DeliveryException.DeliveryNotFound 배송을 찾을 수 없을 때
     */
    @Transactional(readOnly = true)
    fun getDeliveryByTrackingNumber(trackingNumber: String): Delivery {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
            ?: throw DeliveryException.DeliveryNotFound(0L)
    }
}
