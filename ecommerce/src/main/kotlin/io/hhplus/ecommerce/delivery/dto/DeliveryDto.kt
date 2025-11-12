package io.hhplus.ecommerce.delivery.dto

import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import java.time.LocalDateTime

/**
 * 배송지 정보 요청 DTO
 *
 * 역할:
 * - 주문 생성 시 배송지 정보를 전달
 * - DeliveryAddress VO로 변환
 */
data class DeliveryAddressRequest(
    val recipientName: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val addressDetail: String? = null,
    val deliveryMessage: String? = null
) {
    /**
     * DeliveryAddress VO로 변환
     */
    fun toVo(): DeliveryAddress {
        return DeliveryAddress.create(
            recipientName = recipientName,
            phone = phone,
            zipCode = zipCode,
            address = address,
            addressDetail = addressDetail,
            deliveryMessage = deliveryMessage
        )
    }
}

/**
 * 배송지 정보 응답 DTO
 */
data class DeliveryAddressResponse(
    val recipientName: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val addressDetail: String?,
    val deliveryMessage: String?
) {
    companion object {
        fun from(deliveryAddress: DeliveryAddress): DeliveryAddressResponse {
            return DeliveryAddressResponse(
                recipientName = deliveryAddress.recipientName,
                phone = deliveryAddress.phone,
                zipCode = deliveryAddress.zipCode,
                address = deliveryAddress.address,
                addressDetail = deliveryAddress.addressDetail,
                deliveryMessage = deliveryAddress.deliveryMessage
            )
        }
    }
}

/**
 * 배송 정보 응답 DTO
 *
 * 역할:
 * - Delivery 엔티티를 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 */
data class DeliveryResponse(
    val id: Long,
    val orderId: Long,
    val deliveryAddress: DeliveryAddressResponse,
    val trackingNumber: String?,
    val carrier: String?,
    val status: DeliveryStatus,
    val shippedAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val deliveryMemo: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(delivery: Delivery): DeliveryResponse {
            return DeliveryResponse(
                id = delivery.id,
                orderId = delivery.orderId,
                deliveryAddress = DeliveryAddressResponse.from(delivery.deliveryAddress),
                trackingNumber = delivery.trackingNumber,
                carrier = delivery.carrier,
                status = delivery.status,
                shippedAt = delivery.shippedAt,
                deliveredAt = delivery.deliveredAt,
                deliveryMemo = delivery.deliveryMemo,
                createdAt = delivery.createdAt,
                updatedAt = delivery.updatedAt
            )
        }
    }
}

/**
 * 배송 발송 요청 DTO
 */
data class ShipDeliveryRequest(
    val trackingNumber: String,
    val carrier: String,
    val shippedBy: Long
)

/**
 * 배송 완료 요청 DTO
 */
data class DeliverDeliveryRequest(
    val deliveredBy: Long
)

/**
 * 배송 상태 변경 요청 DTO
 */
data class UpdateDeliveryStatusRequest(
    val status: DeliveryStatus,
    val updatedBy: Long
)

/**
 * Delivery 엔티티 확장 함수 - 응답 DTO 변환
 */
fun Delivery.toResponse(): DeliveryResponse = DeliveryResponse.from(this)
