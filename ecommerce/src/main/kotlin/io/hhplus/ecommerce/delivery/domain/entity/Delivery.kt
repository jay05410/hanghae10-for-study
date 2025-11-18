package io.hhplus.ecommerce.delivery.domain.entity

import io.hhplus.ecommerce.delivery.exception.DeliveryException
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import java.time.LocalDateTime

/**
 * 배송 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 주문에 대한 배송 정보 관리
 * - 배송 상태 전환 및 추적
 * - 배송지 정보 스냅샷 저장
 *
 * 비즈니스 규칙:
 * - 주문 1건당 배송 1건 (1:1 관계)
 * - 배송 상태는 단방향으로만 전환 가능
 * - 발송 시 운송장 번호와 택배사 정보 필수
 * - 배송지 정보는 주문 시점 스냅샷으로 저장 (불변)
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/DeliveryJpaEntity에서 처리됩니다.
 */
data class Delivery(
    val id: Long = 0,
    val orderId: Long,
    val deliveryAddress: DeliveryAddress,
    var trackingNumber: String? = null,
    var carrier: String? = null,
    var status: DeliveryStatus = DeliveryStatus.PENDING,
    var shippedAt: LocalDateTime? = null,
    var deliveredAt: LocalDateTime? = null,
    val deliveryMemo: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0
) {

    /**
     * 배송 상태를 변경
     *
     * @param newStatus 새로운 배송 상태
     * @param updatedBy 상태 변경자 ID
     * @return 상태가 변경된 새로운 Delivery 인스턴스
     * @throws DeliveryException.InvalidDeliveryStateTransition 잘못된 상태 전환 시도 시
     */
    fun updateStatus(newStatus: DeliveryStatus, updatedBy: Long): Delivery {
        if (!status.canTransitionTo(newStatus)) {
            throw DeliveryException.InvalidDeliveryStateTransition(status, newStatus)
        }
        return this.copy(
            status = newStatus,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 배송 준비 시작
     *
     * @param updatedBy 상태 변경자 ID
     * @return 상태가 변경된 새로운 Delivery 인스턴스
     * @throws DeliveryException.InvalidDeliveryStateTransition 잘못된 상태 전환 시도 시
     */
    fun startPreparing(updatedBy: Long): Delivery {
        return updateStatus(DeliveryStatus.PREPARING, updatedBy)
    }

    /**
     * 배송 발송 처리 (운송장 등록)
     *
     * @param trackingNumber 운송장 번호
     * @param carrier 택배사
     * @param updatedBy 상태 변경자 ID
     * @param shippedAt 발송 일시 (기본값: 현재 시간)
     * @return 발송 처리된 새로운 Delivery 인스턴스
     * @throws DeliveryException.InvalidDeliveryStateTransition 잘못된 상태일 때
     * @throws IllegalArgumentException 필수 정보 누락 시
     */
    fun ship(
        trackingNumber: String,
        carrier: String,
        updatedBy: Long,
        shippedAt: LocalDateTime = LocalDateTime.now()
    ): Delivery {
        require(trackingNumber.isNotBlank()) { "운송장 번호는 필수입니다" }
        require(carrier.isNotBlank()) { "택배사 정보는 필수입니다" }

        if (!status.canTransitionTo(DeliveryStatus.SHIPPED)) {
            throw DeliveryException.InvalidDeliveryStateTransition(status, DeliveryStatus.SHIPPED)
        }

        return this.copy(
            trackingNumber = trackingNumber,
            carrier = carrier,
            shippedAt = shippedAt,
            status = DeliveryStatus.SHIPPED,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 배송 완료 처리
     *
     * @param updatedBy 상태 변경자 ID
     * @param deliveredAt 배송 완료 일시 (기본값: 현재 시간)
     * @return 배송 완료 처리된 새로운 Delivery 인스턴스
     * @throws DeliveryException.InvalidDeliveryStateTransition 잘못된 상태일 때
     */
    fun deliver(updatedBy: Long, deliveredAt: LocalDateTime = LocalDateTime.now()): Delivery {
        if (!status.canTransitionTo(DeliveryStatus.DELIVERED)) {
            throw DeliveryException.InvalidDeliveryStateTransition(status, DeliveryStatus.DELIVERED)
        }

        return this.copy(
            deliveredAt = deliveredAt,
            status = DeliveryStatus.DELIVERED,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 배송 실패 처리
     *
     * @param updatedBy 상태 변경자 ID
     * @return 배송 실패 처리된 새로운 Delivery 인스턴스
     * @throws DeliveryException.InvalidDeliveryStateTransition 잘못된 상태일 때
     */
    fun fail(updatedBy: Long): Delivery {
        return updateStatus(DeliveryStatus.FAILED, updatedBy)
    }

    /**
     * 배송지 변경 가능 여부 확인
     */
    fun canChangeAddress(): Boolean = status.canChangeAddress()

    /**
     * 반품 가능 여부 확인 (배송 완료 후 7일 이내)
     */
    fun canReturn(currentDate: LocalDateTime = LocalDateTime.now()): Boolean {
        if (!status.canReturn()) return false
        val deliveredDate = deliveredAt ?: return false
        val daysDifference = java.time.Duration.between(deliveredDate, currentDate).toDays()
        return daysDifference <= 7
    }

    companion object {
        /**
         * 배송 생성 팩토리 메서드
         *
         * @param orderId 주문 ID
         * @param deliveryAddress 배송지 정보
         * @param deliveryMemo 배송 메모
         * @param createdBy 생성자 ID
         * @return 생성된 Delivery 도메인 모델
         */
        fun create(
            orderId: Long,
            deliveryAddress: DeliveryAddress,
            deliveryMemo: String? = null,
            createdBy: Long
        ): Delivery {
            require(orderId > 0) { "주문 ID는 유효해야 합니다" }

            val now = LocalDateTime.now()
            return Delivery(
                orderId = orderId,
                deliveryAddress = deliveryAddress,
                deliveryMemo = deliveryMemo,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}