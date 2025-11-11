package io.hhplus.ecommerce.product.dto

import io.hhplus.ecommerce.product.domain.entity.Product
import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: Long,
    val categoryId: Long,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

fun Product.toResponse(): ProductResponse = ProductResponse(
    id = this.id,
    name = this.name,
    description = this.description,
    price = this.price,
    categoryId = this.categoryId,
    isActive = this.isActive,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)