package io.hhplus.ecommerce.point.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 사용 요청")
data class DeductPointRequest(
    @Schema(description = "사용할 포인트 금액 (양수)", example = "500", minimum = "1")
    val amount: Long,

    @Schema(description = "사용 사유 (선택)", example = "상품 구매")
    val description: String? = null
)