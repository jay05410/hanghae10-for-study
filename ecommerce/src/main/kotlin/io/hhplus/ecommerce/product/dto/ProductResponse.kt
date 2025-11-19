package io.hhplus.ecommerce.product.dto

import io.hhplus.ecommerce.product.domain.entity.Product
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 정보")
data class ProductResponse(
    @Schema(description = "상품 ID", example = "1")
    val id: Long,

    @Schema(description = "상품명", example = "프리미엄 커피")
    val name: String,

    @Schema(description = "상품 설명", example = "고급 원두로 만든 신선한 커피")
    val description: String,

    @Schema(description = "가격", example = "15000")
    val price: Long,

    @Schema(description = "카테고리 ID", example = "1")
    val categoryId: Long
)

fun Product.toResponse(): ProductResponse = ProductResponse(
    id = this.id,
    name = this.name,
    description = this.description,
    price = this.price,
    categoryId = this.categoryId
)