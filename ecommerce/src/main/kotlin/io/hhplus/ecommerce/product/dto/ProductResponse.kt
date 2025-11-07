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
) {
    companion object {
        fun from(product: Product): ProductResponse {
            return ProductResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price,
                categoryId = product.categoryId,
                isActive = product.isActive,
                createdAt = product.createdAt,
                updatedAt = product.updatedAt
            )
        }
    }
}