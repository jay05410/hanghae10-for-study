package io.hhplus.ecommerce.domain.cart.vo

/**
 * 수량 Value Object
 *
 * 비즈니스 규칙:
 * - 양수여야 함
 *
 * @property value 수량
 */
@JvmInline
value class Quantity private constructor(val value: Int) {

    companion object {
        /**
         * Quantity 생성
         *
         * @param quantity 수량
         * @return 검증된 Quantity 객체
         * @throws IllegalArgumentException 유효하지 않은 수량
         */
        operator fun invoke(quantity: Int): Quantity {
            // 양수 검증: 0개 또는 음수는 의미상 불가능
            require(quantity > 0) { "수량은 0보다 커야 합니다: $quantity" }
            return Quantity(quantity)
        }
    }
}