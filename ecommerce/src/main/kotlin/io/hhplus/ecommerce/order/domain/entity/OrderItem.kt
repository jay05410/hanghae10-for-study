package io.hhplus.ecommerce.order.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
//import jakarta.persistence.*

//@Entity
//@Table(name = "order_item")
class OrderItem(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @Column(nullable = false)
    val orderId: Long,

//    @Column(nullable = false)
    val packageTypeId: Long,

//    @Column(nullable = false, length = 100)
    val packageTypeName: String,

//    @Column(nullable = false)
    val packageTypeDays: Int,

//    @Column(nullable = false)
    val dailyServing: Int,

//    @Column(nullable = false)
    val totalQuantity: Double,

//    @Column(nullable = false)
    val giftWrap: Boolean = false,

//    @Column(length = 500)
    val giftMessage: String? = null,

//    @Column(nullable = false)
    val quantity: Int,

//    @Column(nullable = false)
    val containerPrice: Int,

//    @Column(nullable = false)
    val teaPrice: Int,

//    @Column(nullable = false)
    val giftWrapPrice: Int = 0,

//    @Column(nullable = false)
    val totalPrice: Int
) : ActiveJpaEntity() {
    fun validatePrices() {
        require(containerPrice >= 0) { "용기 가격은 0 이상이어야 합니다: $containerPrice" }
        require(teaPrice >= 0) { "차 가격은 0 이상이어야 합니다: $teaPrice" }
        require(giftWrapPrice >= 0) { "선물포장 가격은 0 이상이어야 합니다: $giftWrapPrice" }
        require(totalPrice == (containerPrice + teaPrice + giftWrapPrice) * quantity) {
            "총 가격이 올바르지 않습니다: 계산값=${(containerPrice + teaPrice + giftWrapPrice) * quantity}, 실제값=$totalPrice"
        }
    }

    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use packageTypeId instead", ReplaceWith("packageTypeId"))
    val boxTypeId: Long
        get() = packageTypeId

    @Deprecated("Use teaPrice instead", ReplaceWith("teaPrice.toLong()"))
    val unitPrice: Long
        get() = teaPrice.toLong()

    companion object {
        fun create(
            orderId: Long,
            packageTypeId: Long,
            packageTypeName: String,
            packageTypeDays: Int,
            dailyServing: Int,
            totalQuantity: Double,
            giftWrap: Boolean = false,
            giftMessage: String? = null,
            quantity: Int,
            containerPrice: Int,
            teaPrice: Int,
            giftWrapPrice: Int = 0
        ): OrderItem {
            require(orderId >= 0) { "주문 ID는 유효해야 합니다" }
            require(packageTypeId > 0) { "박스 타입 ID는 유효해야 합니다" }
            require(packageTypeName.isNotBlank()) { "박스 타입명은 필수입니다" }
            require(packageTypeDays > 0) { "일수는 0보다 커야 합니다" }
            require(dailyServing in 1..3) { "하루 섭취량은 1-3 사이여야 합니다" }
            require(totalQuantity > 0) { "총 그램수는 0보다 커야 합니다" }
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(containerPrice >= 0) { "용기 가격은 0 이상이어야 합니다" }
            require(teaPrice >= 0) { "차 가격은 0 이상이어야 합니다" }
            require(giftWrapPrice >= 0) { "선물포장 가격은 0 이상이어야 합니다" }

            val totalPrice = (containerPrice + teaPrice + giftWrapPrice) * quantity

            return OrderItem(
                orderId = orderId,
                packageTypeId = packageTypeId,
                packageTypeName = packageTypeName,
                packageTypeDays = packageTypeDays,
                dailyServing = dailyServing,
                totalQuantity = totalQuantity,
                giftWrap = giftWrap,
                giftMessage = giftMessage,
                quantity = quantity,
                containerPrice = containerPrice,
                teaPrice = teaPrice,
                giftWrapPrice = giftWrapPrice,
                totalPrice = totalPrice
            ).also { it.validatePrices() }
        }
    }
}