package io.hhplus.ecommerce.order.domain.model

/**
 * 주문 아이템 데이터 - 도메인 모델
 *
 * 역할:
 * - 주문 생성 시 필요한 아이템 정보를 담는 VO
 * - UseCase와 DomainService 간 데이터 전달
 */
data class OrderItemData(
    val productId: Long,
    val productName: String,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val giftWrapPrice: Int = 0,
    val totalPrice: Int,
    val requiresReservation: Boolean = false
)
