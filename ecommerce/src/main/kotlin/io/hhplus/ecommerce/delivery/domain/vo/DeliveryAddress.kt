package io.hhplus.ecommerce.delivery.domain.vo

import kotlinx.serialization.Serializable

/**
 * 배송지 정보 Value Object
 *
 * 역할:
 * - 주문 시점의 배송지 정보를 스냅샷으로 저장
 * - JSON 형태로 직렬화하여 DELIVERY 테이블의 delivery_address 컬럼에 저장
 * - 불변 객체로 관리하여 배송지 정보의 일관성 보장
 *
 * 비즈니스 규칙:
 * - 배송지 정보는 주문 시점에 스냅샷으로 저장되어야 함
 * - 사용자가 나중에 배송지를 수정/삭제해도 주문의 배송지는 변경되지 않음
 */
@Serializable
data class DeliveryAddress(
    /** 수령인 이름 */
    val recipientName: String,

    /** 연락처 */
    val phone: String,

    /** 우편번호 */
    val zipCode: String,

    /** 기본 주소 */
    val address: String,

    /** 상세 주소 */
    val addressDetail: String? = null,

    /** 배송 메시지 */
    val deliveryMessage: String? = null
) {
    init {
        require(recipientName.isNotBlank()) { "수령인 이름은 필수입니다" }
        require(phone.isNotBlank()) { "연락처는 필수입니다" }
        require(zipCode.isNotBlank()) { "우편번호는 필수입니다" }
        require(address.isNotBlank()) { "주소는 필수입니다" }
    }

    /**
     * 배송지 정보를 한 줄로 표현
     */
    fun toFullAddress(): String {
        val detail = addressDetail?.let { " $it" } ?: ""
        return "[$zipCode] $address$detail"
    }

    /**
     * 배송 정보 요약 (수령인 + 주소)
     */
    fun toSummary(): String {
        return "$recipientName / ${toFullAddress()}"
    }

    companion object {
        /**
         * 배송지 정보 생성 팩토리 메서드
         */
        fun create(
            recipientName: String,
            phone: String,
            zipCode: String,
            address: String,
            addressDetail: String? = null,
            deliveryMessage: String? = null
        ): DeliveryAddress {
            return DeliveryAddress(
                recipientName = recipientName.trim(),
                phone = phone.trim(),
                zipCode = zipCode.trim(),
                address = address.trim(),
                addressDetail = addressDetail?.trim(),
                deliveryMessage = deliveryMessage?.trim()
            )
        }
    }
}
