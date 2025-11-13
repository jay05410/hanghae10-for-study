package io.hhplus.ecommerce.support.config

import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.order.dto.OrderItemData
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
     * 테스트용 박스타입 ID 생성
     */
    fun createTestBoxTypeId(seed: Int = 1): Long = 10L + seed

    /**
     * 테스트용 TeaItemRequest 생성
     */
    fun createTeaItemRequest(
        productId: Long = 1L,
        selectionOrder: Int = 1,
        ratioPercent: Int = 100
    ) = TeaItemRequest(
        productId = productId,
        selectionOrder = selectionOrder,
        ratioPercent = ratioPercent
    )

    /**
     * 테스트용 OrderItemData 생성
     */
    fun createOrderItemData(
        packageTypeId: Long = 1L,
        packageTypeName: String = "테스트 패키지",
        packageTypeDays: Int = 7,
        dailyServing: Int = 1,
        totalQuantity: Double = 7.0,
        giftWrap: Boolean = false,
        giftMessage: String? = null,
        quantity: Int = 1,
        containerPrice: Int = 5000,
        teaPrice: Int = 20000,
        giftWrapPrice: Int = 0,
        teaItems: List<TeaItemRequest> = emptyList()
    ) = OrderItemData(
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
        teaItems = teaItems
    )

    /**
     * 현재 시간 생성 (테스트용)
     */
    fun now(): LocalDateTime = LocalDateTime.now()
}
