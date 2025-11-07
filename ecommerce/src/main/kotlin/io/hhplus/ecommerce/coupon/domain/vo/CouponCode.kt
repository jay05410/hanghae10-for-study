package io.hhplus.ecommerce.coupon.domain.vo

@JvmInline
value class CouponCode private constructor(val value: String) {
    init {
        require(value.isNotBlank()) { "쿠폰 코드는 비어있을 수 없습니다" }
        require(value.length >= MIN_LENGTH) { "쿠폰 코드는 최소 ${MIN_LENGTH}자 이상이어야 합니다" }
        require(value.length <= MAX_LENGTH) { "쿠폰 코드는 최대 ${MAX_LENGTH}자 이하여야 합니다" }
        require(value.matches(CODE_PATTERN)) { "쿠폰 코드는 영문 대문자와 숫자만 포함할 수 있습니다" }
    }

    companion object {
        private const val MIN_LENGTH = 6
        private const val MAX_LENGTH = 20
        private val CODE_PATTERN = Regex("^[A-Z0-9]+$")

        fun of(value: String): CouponCode = CouponCode(value.uppercase().trim())
    }
}