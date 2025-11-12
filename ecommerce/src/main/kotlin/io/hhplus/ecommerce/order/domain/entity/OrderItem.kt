package io.hhplus.ecommerce.order.domain.entity

import java.time.LocalDateTime

/**
 * OrderItem 도메인 모델 (immutable)
 *
 * 역할:
 * - 주문 아이템의 정보 보관
 * - JPA 의존성 제거로 도메인 순수성 유지
 */
data class OrderItem(
    val id: Long = 0,
    val orderId: Long,
    val packageTypeId: Long,
    val packageTypeName: String,
    val packageTypeDays: Int,
    val dailyServing: Int,
    val totalQuantity: Double,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val quantity: Int,
    val containerPrice: Int,
    val teaPrice: Int,
    val giftWrapPrice: Int = 0,
    val totalPrice: Int,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val deletedAt: LocalDateTime? = null
) {
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