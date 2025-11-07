package io.hhplus.ecommerce.product.domain.vo

/**
 * 상품 가격 Value Object
 *
 * 비즈니스 규칙:
 * - 양수여야 함
 *
 * @property value 100g당 가격
 */
@JvmInline
value class ProductPrice private constructor(val value: Int) {

    fun calculateTotalPrice(quantity: Int): Long {
        require(quantity > 0) { "수량은 0보다 커야 합니다" }
        return value.toLong() * quantity
    }

    fun getFormattedPrice(): String = "${String.format("%,d", value)}원"

    companion object {
        /**
         * ProductPrice 생성
         *
         * @param price 100g당 가격
         * @return 검증된 ProductPrice 객체
         * @throws IllegalArgumentException 유효하지 않은 가격
         */
        operator fun invoke(price: Int): ProductPrice {
            require(price > 0) { "가격은 0보다 커야 합니다: $price" }
            return ProductPrice(price)
        }
    }
}