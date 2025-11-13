package io.hhplus.ecommerce.delivery.dto

import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 배송지 정보 요청 DTO
 *
 * 역할:
 * - 주문 생성 시 배송지 정보를 전달
 * - DeliveryAddress VO로 변환
 */
@Schema(description = "배송지 정보 요청")
data class DeliveryAddressRequest(
    @Schema(description = "수령인 이름", example = "홍길동", required = true)
    val recipientName: String,

    @Schema(description = "연락처", example = "010-1234-5678", required = true)
    val phone: String,

    @Schema(description = "우편번호", example = "12345", required = true)
    val zipCode: String,

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123", required = true)
    val address: String,

    @Schema(description = "상세 주소 (선택)", example = "501호")
    val addressDetail: String? = null,

    @Schema(description = "배송 메시지 (선택)", example = "문 앞에 놓아주세요")
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
@Schema(description = "배송지 정보")
data class DeliveryAddressResponse(
    @Schema(description = "수령인 이름", example = "홍길동")
    val recipientName: String,

    @Schema(description = "연락처", example = "010-1234-5678")
    val phone: String,

    @Schema(description = "우편번호", example = "12345")
    val zipCode: String,

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    val address: String,

    @Schema(description = "상세 주소 (선택)", example = "501호")
    val addressDetail: String?,

    @Schema(description = "배송 메시지 (선택)", example = "문 앞에 놓아주세요")
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
@Schema(description = "배송 정보")
data class DeliveryResponse(
    @Schema(description = "배송 ID", example = "1")
    val id: Long,

    @Schema(description = "주문 ID", example = "100")
    val orderId: Long,

    @Schema(description = "배송지 정보")
    val deliveryAddress: DeliveryAddressResponse,

    @Schema(description = "송장 번호 (선택)", example = "1234567890")
    val trackingNumber: String?,

    @Schema(description = "택배사 (선택)", example = "CJ대한통운")
    val carrier: String?,

    @Schema(description = "배송 상태", example = "PENDING", allowableValues = ["PENDING", "SHIPPED", "IN_TRANSIT", "DELIVERED", "CANCELLED"])
    val status: DeliveryStatus,

    @Schema(description = "발송 일시 (선택)", example = "2025-01-14T10:00:00")
    val shippedAt: LocalDateTime?,

    @Schema(description = "배송 완료 일시 (선택)", example = "2025-01-16T14:30:00")
    val deliveredAt: LocalDateTime?,

    @Schema(description = "배송 메모 (선택)", example = "안전하게 배송되었습니다")
    val deliveryMemo: String?,

    @Schema(description = "생성 일시", example = "2025-01-13T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 일시", example = "2025-01-14T10:00:00")
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
@Schema(description = "배송 발송 요청")
data class ShipDeliveryRequest(
    @Schema(description = "송장 번호", example = "1234567890", required = true)
    val trackingNumber: String,

    @Schema(description = "택배사", example = "CJ대한통운", required = true)
    val carrier: String,

    @Schema(description = "발송 처리자 ID", example = "1", required = true)
    val shippedBy: Long
)

/**
 * 배송 완료 요청 DTO
 */
@Schema(description = "배송 완료 요청")
data class DeliverDeliveryRequest(
    @Schema(description = "배송 완료 처리자 ID", example = "1", required = true)
    val deliveredBy: Long
)

/**
 * 배송 상태 변경 요청 DTO
 */
@Schema(description = "배송 상태 변경 요청")
data class UpdateDeliveryStatusRequest(
    @Schema(description = "변경할 배송 상태", example = "SHIPPED", required = true, allowableValues = ["PENDING", "SHIPPED", "IN_TRANSIT", "DELIVERED", "CANCELLED"])
    val status: DeliveryStatus,

    @Schema(description = "변경 처리자 ID", example = "1", required = true)
    val updatedBy: Long
)

/**
 * Delivery 엔티티 확장 함수 - 응답 DTO 변환
 */
fun Delivery.toResponse(): DeliveryResponse = DeliveryResponse.from(this)
