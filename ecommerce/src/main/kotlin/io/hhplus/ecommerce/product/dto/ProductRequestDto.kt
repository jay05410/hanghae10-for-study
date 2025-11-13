package io.hhplus.ecommerce.product.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 생성 요청")
data class CreateProductRequest(
    @Schema(description = "상품명", example = "프리미엄 커피", required = true)
    val name: String,

    @Schema(description = "상품 설명", example = "고급 원두로 만든 신선한 커피", required = true)
    val description: String,

    @Schema(description = "가격", example = "15000", required = true)
    val price: Long,

    @Schema(description = "카테고리 ID", example = "1", required = true)
    val categoryId: Long,

    @Schema(description = "생성자 ID", example = "1", required = true)
    val createdBy: Long
)

@Schema(description = "상품 정보 수정 요청")
data class UpdateProductRequest(
    @Schema(description = "상품명", example = "프리미엄 커피", required = true)
    val name: String,

    @Schema(description = "상품 설명", example = "고급 원두로 만든 신선한 커피", required = true)
    val description: String,

    @Schema(description = "가격", example = "15000", required = true)
    val price: Long,

    @Schema(description = "수정자 ID", example = "1", required = true)
    val updatedBy: Long
)

@Schema(description = "상품 검색 요청")
data class ProductSearchRequest(
    @Schema(description = "카테고리 ID (선택)", example = "1")
    val categoryId: Long?,

    @Schema(description = "검색 키워드 (선택)", example = "커피")
    val keyword: String?,

    @Schema(description = "최소 가격 (선택)", example = "10000")
    val priceMin: Long?,

    @Schema(description = "최대 가격 (선택)", example = "50000")
    val priceMax: Long?,

    @Schema(description = "페이지 번호", example = "0", defaultValue = "0")
    val page: Int = 0,

    @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
    val size: Int = 20
)

@Schema(description = "재고 예약 요청")
data class StockReservationRequest(
    @Schema(description = "상품 ID", example = "10", required = true)
    val productId: Long,

    @Schema(description = "박스 타입 ID", example = "5", required = true)
    val boxTypeId: Long,

    @Schema(description = "예약 수량", example = "3", required = true)
    val quantity: Int
)