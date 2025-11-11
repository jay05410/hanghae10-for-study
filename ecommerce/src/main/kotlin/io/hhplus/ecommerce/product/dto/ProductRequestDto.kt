package io.hhplus.ecommerce.product.dto

data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Long,
    val categoryId: Long,
    val createdBy: Long
)

data class UpdateProductRequest(
    val name: String,
    val description: String,
    val price: Long,
    val updatedBy: Long
)

data class ProductSearchRequest(
    val categoryId: Long?,
    val keyword: String?,
    val priceMin: Long?,
    val priceMax: Long?,
    val page: Int = 0,
    val size: Int = 20
)

data class StockReservationRequest(
    val productId: Long,
    val boxTypeId: Long,
    val quantity: Int
)