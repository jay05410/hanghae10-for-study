package io.hhplus.ecommerce.delivery.usecase

import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.repository.DeliveryRepository
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import io.hhplus.ecommerce.common.exception.delivery.DeliveryException
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
    private val deliveryRepository: DeliveryRepository
) {

    private fun getDelivery(id: Long): Delivery {
        return deliveryRepository.findById(id)
            ?: throw DeliveryException.DeliveryNotFound(id)
    }

    /**
     * 새로운 배송 정보를 생성합니다.
     *
     * @param orderId 주문 ID
     * @param deliveryAddress 배송지 정보
     * @param deliveryMemo 배송 메모
     * @param createdBy 생성 요청자 ID
     * @return 생성된 배송 정보
     * @throws IllegalStateException 이미 배송 정보가 존재하는 경우
     */
    @Transactional
    fun createDelivery(
        orderId: Long,
        deliveryAddress: DeliveryAddress,
        deliveryMemo: String? = null,
        createdBy: Long = 1L
    ): Delivery {
        // 주문에 대한 배송이 이미 존재하는지 확인 (1:1 관계)
        deliveryRepository.findByOrderId(orderId)?.let {
            throw IllegalStateException("주문 ID $orderId 에 대한 배송 정보가 이미 존재합니다")
        }

        val delivery = Delivery.create(
            orderId = orderId,
            deliveryAddress = deliveryAddress,
            deliveryMemo = deliveryMemo,
            createdBy = createdBy
        )

        return deliveryRepository.save(delivery)
    }

    /**
     * 배송 준비를 시작합니다.
     *
     * @param id 배송 ID
     * @param updatedBy 업데이트 요청자 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun startPreparing(id: Long, updatedBy: Long = 1L): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.startPreparing(updatedBy)
        return deliveryRepository.save(updated)
    }

    /**
     * 배송을 시작합니다.
     *
     * @param id 배송 ID
     * @param trackingNumber 운송장 번호
     * @param carrier 택배사
     * @param updatedBy 업데이트 요청자 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun ship(
        id: Long,
        trackingNumber: String,
        carrier: String,
        updatedBy: Long = 1L
    ): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.ship(trackingNumber, carrier, updatedBy)
        return deliveryRepository.save(updated)
    }

    /**
     * 배송을 완료합니다.
     *
     * @param id 배송 ID
     * @param updatedBy 업데이트 요청자 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun deliver(id: Long, updatedBy: Long = 1L): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.deliver(updatedBy)
        return deliveryRepository.save(updated)
    }

    /**
     * 배송을 실패 처리합니다.
     *
     * @param id 배송 ID
     * @param updatedBy 업데이트 요청자 ID
     * @return 업데이트된 배송 정보
     */
    @Transactional
    fun fail(id: Long, updatedBy: Long = 1L): Delivery {
        val delivery = getDelivery(id)
        val updated = delivery.fail(updatedBy)
        return deliveryRepository.save(updated)
    }
}