package io.hhplus.ecommerce.delivery.domain.service

import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.repository.DeliveryRepository
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import io.hhplus.ecommerce.delivery.exception.DeliveryException
import org.springframework.stereotype.Component

/**
 * 배송 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 배송 엔티티 생성 및 상태 관리
 * - 중복 배송 검증
 * - 배송 상태 전환
 *
 * 책임:
 * - 배송 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - @Transactional 사용 금지
 */
@Component
class DeliveryDomainService(
    private val deliveryRepository: DeliveryRepository
) {

    /**
     * 중복 배송 검증
     *
     * @param orderId 주문 ID
     * @throws IllegalStateException 이미 배송이 존재하는 경우
     */
    fun validateNoDuplicateDelivery(orderId: Long) {
        deliveryRepository.findByOrderId(orderId)?.let {
            throw IllegalStateException("주문 ID $orderId 에 대한 배송 정보가 이미 존재합니다")
        }
    }

    /**
     * 새로운 배송 생성
     *
     * @param orderId 주문 ID
     * @param deliveryAddress 배송지 정보
     * @param deliveryMemo 배송 메모
     * @return 생성된 배송 엔티티
     */
    fun createDelivery(
        orderId: Long,
        deliveryAddress: DeliveryAddress,
        deliveryMemo: String?
    ): Delivery {
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
    fun getDelivery(id: Long): Delivery {
        return deliveryRepository.findById(id)
            ?: throw DeliveryException.DeliveryNotFound(id)
    }

    /**
     * 배송 ID로 배송 정보 조회 (Optional)
     *
     * @param id 배송 ID
     * @return 배송 엔티티 (없으면 null)
     */
    fun getDeliveryOrNull(id: Long): Delivery? {
        return deliveryRepository.findById(id)
    }

    /**
     * 주문 ID로 배송 정보 조회
     *
     * @param orderId 주문 ID
     * @return 배송 엔티티
     * @throws DeliveryException.DeliveryNotFoundByOrder 배송을 찾을 수 없을 때
     */
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
    fun getDeliveriesByOrderIds(orderIds: List<Long>): List<Delivery> {
        return deliveryRepository.findByOrderIdIn(orderIds)
    }

    /**
     * 배송 준비 시작
     *
     * @param delivery 배송 엔티티
     * @return 업데이트된 배송 엔티티
     */
    fun startPreparing(delivery: Delivery): Delivery {
        val updated = delivery.startPreparing()
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 발송 처리 (운송장 등록)
     *
     * @param delivery 배송 엔티티
     * @param trackingNumber 운송장 번호
     * @param carrier 택배사
     * @return 업데이트된 배송 엔티티
     */
    fun ship(
        delivery: Delivery,
        trackingNumber: String,
        carrier: String
    ): Delivery {
        val updated = delivery.ship(trackingNumber, carrier)
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 완료 처리
     *
     * @param delivery 배송 엔티티
     * @return 업데이트된 배송 엔티티
     */
    fun deliver(delivery: Delivery): Delivery {
        val updated = delivery.deliver()
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 실패 처리
     *
     * @param delivery 배송 엔티티
     * @return 업데이트된 배송 엔티티
     */
    fun fail(delivery: Delivery): Delivery {
        val updated = delivery.fail()
        return deliveryRepository.save(updated)
    }

    /**
     * 배송 상태로 배송 목록 조회
     *
     * @param status 배송 상태
     * @return 배송 엔티티 목록
     */
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
    fun getDeliveryByTrackingNumber(trackingNumber: String): Delivery {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
            ?: throw DeliveryException.DeliveryNotFound(0L)
    }
}