package io.hhplus.ecommerce.cart.domain.entity

import java.time.LocalDateTime

/**
 * 장바구니 아이템 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 장바구니에 담긴 개별 상품 정보 관리
 * - 패키지 타입, 수량, 선물 옵션 등 관리
 *
 * 비즈니스 규칙:
 * - 하루 섭취량은 1-3 사이
 * - 총 그램수는 0보다 커야 함
 * - 선물 포장 시 선물 메시지 선택 가능
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/CartItemJpaEntity에서 처리됩니다.
 */
data class CartItem(
    val id: Long = 0,
    val cartId: Long,
    val packageTypeId: Long,
    val packageTypeName: String,
    val packageTypeDays: Int,
    val dailyServing: Int = 1,
    var totalQuantity: Double,
    val giftWrap: Boolean = false,
    val giftMessage: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0
) {
    fun validateTotalQuantity() {
        require(totalQuantity > 0) { "총 그램수는 0보다 커야 합니다: $totalQuantity" }
    }

    fun updateQuantity(newQuantity: Double) {
        require(newQuantity > 0) { "총 그램수는 0보다 커야 합니다: $newQuantity" }
        this.totalQuantity = newQuantity
        this.updatedAt = LocalDateTime.now()
    }

    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use packageTypeId instead", ReplaceWith("packageTypeId"))
    val boxTypeId: Long
        get() = packageTypeId

    @Deprecated("Use totalQuantity instead", ReplaceWith("totalQuantity.toInt()"))
    val quantity: Int
        get() = totalQuantity.toInt()

    companion object {
        /**
         * 장바구니 아이템 생성 팩토리 메서드
         *
         * @return 생성된 CartItem 도메인 모델
         */
        fun create(
            cartId: Long,
            packageTypeId: Long,
            packageTypeName: String,
            packageTypeDays: Int,
            dailyServing: Int,
            totalQuantity: Double,
            giftWrap: Boolean = false,
            giftMessage: String? = null
        ): CartItem {
            require(cartId > 0) { "장바구니 ID는 유효해야 합니다" }
            require(packageTypeId > 0) { "박스 타입 ID는 유효해야 합니다" }
            require(packageTypeName.isNotBlank()) { "박스 타입명은 필수입니다" }
            require(packageTypeDays > 0) { "일수는 0보다 커야 합니다" }
            require(dailyServing in 1..3) { "하루 섭취량은 1-3 사이여야 합니다" }
            require(totalQuantity > 0) { "총 그램수는 0보다 커야 합니다" }

            val now = LocalDateTime.now()
            return CartItem(
                cartId = cartId,
                packageTypeId = packageTypeId,
                packageTypeName = packageTypeName,
                packageTypeDays = packageTypeDays,
                dailyServing = dailyServing,
                totalQuantity = totalQuantity,
                giftWrap = giftWrap,
                giftMessage = giftMessage,
                createdAt = now,
                updatedAt = now
            ).also { it.validateTotalQuantity() }
        }
    }
}