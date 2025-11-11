package io.hhplus.ecommerce.cart.dto

data class TeaItemRequest(
    val productId: Long,
    val selectionOrder: Int,
    val ratioPercent: Int
) {
    // 이전 버전 호환성을 위한 프로퍼티
    @Deprecated("Use ratioPercent instead", ReplaceWith("ratioPercent"))
    val quantity: Int
        get() = ratioPercent
}