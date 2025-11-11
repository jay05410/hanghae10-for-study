package io.hhplus.ecommerce.delivery.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*
import java.time.LocalDateTime

// @Entity
// @Table(name = "delivery")
class Delivery(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true)
    val orderId: Long,

    // @Column(nullable = false, columnDefinition = "JSON")
    val deliveryAddress: String, // JSON 형태의 배송지 정보 스냅샷

    // @Column(length = 50)
    val trackingNumber: String? = null,

    // @Column(length = 50)
    val carrier: String? = null, // CJ대한통운/우체국/로젠

    // @Column(nullable = false, length = 20)
    val status: String = "PENDING", // PENDING/PREPARING/SHIPPED/DELIVERED/FAILED

    val shippedAt: LocalDateTime? = null,

    val deliveredAt: LocalDateTime? = null,

    // @Column(length = 500)
    val deliveryMemo: String? = null
) : ActiveJpaEntity() {

    fun updateStatus(newStatus: String, updatedAt: LocalDateTime = LocalDateTime.now()) {
        require(newStatus in listOf("PENDING", "PREPARING", "SHIPPED", "DELIVERED", "FAILED")) {
            "유효하지 않은 배송 상태입니다: $newStatus"
        }
    }

    fun ship(trackingNumber: String, carrier: String, shippedAt: LocalDateTime = LocalDateTime.now()) {
        require(status == "PREPARING") { "배송 준비 중 상태에서만 발송 처리 가능합니다" }
        require(trackingNumber.isNotBlank()) { "운송장 번호는 필수입니다" }
        require(carrier.isNotBlank()) { "택배사 정보는 필수입니다" }
    }

    fun deliver(deliveredAt: LocalDateTime = LocalDateTime.now()) {
        require(status == "SHIPPED") { "발송 완료 상태에서만 배송 완료 처리 가능합니다" }
    }

    companion object {
        fun create(
            orderId: Long,
            deliveryAddress: String,
            deliveryMemo: String? = null
        ): Delivery {
            require(orderId > 0) { "주문 ID는 유효해야 합니다" }
            require(deliveryAddress.isNotBlank()) { "배송지 정보는 필수입니다" }

            return Delivery(
                orderId = orderId,
                deliveryAddress = deliveryAddress,
                deliveryMemo = deliveryMemo
            )
        }
    }
}