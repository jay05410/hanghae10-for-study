package io.hhplus.ecommerce.product.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 상품 판매 랭킹 응답 DTO
 */
@Schema(description = "상품 판매 랭킹 정보")
data class ProductRankingResponse(
    @Schema(description = "순위 (1부터 시작)", example = "1")
    val rank: Int,

    @Schema(description = "상품 ID", example = "101")
    val productId: Long,

    @Schema(description = "상품명", example = "프리미엄 차")
    val productName: String,

    @Schema(description = "판매 수량", example = "1523")
    val salesCount: Long
)