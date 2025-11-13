package io.hhplus.ecommerce.point.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 적립 요청")
data class ChargePointRequest(
    @Schema(description = "적립할 포인트 금액 (양수)", example = "1000", minimum = "1")
    val amount: Long,

    @Schema(description = "적립 사유 (선택)", example = "이벤트 참여 보상")
    val description: String? = null
)