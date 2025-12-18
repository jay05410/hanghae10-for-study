package io.hhplus.ecommerce.order.domain.model

/**
 * 주문 아이템 데이터 - 도메인 모델
 *
 * 역할:
 * - 주문 생성 시 필요한 아이템 정보를 담는 VO
 * - UseCase에서 OrderDomainService로 데이터 전달
 *
 * 주의:
 * - 외부 도메인(Pricing 등)에 의존하지 않음
 * - 변환 로직은 Pricing 모듈의 확장함수로 제공
 */
data class OrderItemData(
    val productId: Long,
    val productName: String,
    val categoryId: Long,
    val categoryName: String,
    val quantity: Int,
    val unitPrice: Int,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val giftWrapPrice: Int = 0,
    val totalPrice: Int,
    val requiresReservation: Boolean = false
)
