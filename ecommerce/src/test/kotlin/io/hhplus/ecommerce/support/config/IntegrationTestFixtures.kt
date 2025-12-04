package io.hhplus.ecommerce.support.config

import io.hhplus.ecommerce.order.domain.model.OrderItemData
import java.time.LocalDateTime

/**
 * 통합 테스트용 공통 픽스처 클래스
 *
 * 각 도메인 테스트에서 사용할 공통 테스트 데이터를 제공합니다.
 */
object IntegrationTestFixtures {

    /**
     * 테스트용 사용자 ID 생성
     */
    fun createTestUserId(seed: Int = 1): Long = 1000L + seed

    /**
     * 테스트용 상품 ID 생성
     */
    fun createTestProductId(seed: Int = 1): Long = 100L + seed


    /**
     * 테스트용 OrderItemData 생성
     */
    fun createOrderItemData(
        productId: Long = 1L,
        productName: String = "테스트 상품",
        categoryName: String = "전자기기",
        quantity: Int = 1,
        unitPrice: Int = 10000,
        giftWrap: Boolean = false,
        giftMessage: String? = null,
        giftWrapPrice: Int = 0,
        totalPrice: Int = 10000
    ) = OrderItemData(
        productId = productId,
        productName = productName,
        categoryName = categoryName,
        quantity = quantity,
        unitPrice = unitPrice,
        giftWrap = giftWrap,
        giftMessage = giftMessage,
        giftWrapPrice = giftWrapPrice,
        totalPrice = totalPrice
    )

    /**
     * 현재 시간 생성 (테스트용)
     */
    fun now(): LocalDateTime = LocalDateTime.now()
}
